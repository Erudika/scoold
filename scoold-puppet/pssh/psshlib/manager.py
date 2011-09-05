# Copyright (c) 2009, Andrew McNabb

from errno import EINTR
import os
import select
import signal
import sys
import threading

try:
    import queue
except ImportError:
    import Queue as queue

from psshlib.askpass_server import PasswordServer
from psshlib import psshutil

READ_SIZE = 1 << 16


class FatalError(RuntimeError):
    """A fatal error in the PSSH Manager."""
    pass


class Manager(object):
    """Executes tasks concurrently.

    Tasks are added with add_task() and executed in parallel with run().
    Returns a list of the exit statuses of the processes.

    Arguments:
        limit: Maximum number of commands running at once.
        timeout: Maximum allowed execution time in seconds.
    """
    def __init__(self, opts):
        self.limit = opts.par
        self.timeout = opts.timeout
        self.askpass = opts.askpass
        self.outdir = opts.outdir
        self.errdir = opts.errdir
        self.iomap = IOMap()

        self.taskcount = 0
        self.tasks = []
        self.running = []
        self.done = []

        self.askpass_socket = None

    def run(self):
        """Processes tasks previously added with add_task."""
        try:
            if self.outdir or self.errdir:
                writer = Writer(self.outdir, self.errdir)
                writer.start()
            else:
                writer = None

            if self.askpass:
                pass_server = PasswordServer()
                pass_server.start(self.iomap, self.limit)
                self.askpass_socket = pass_server.address

            self.set_sigchld_handler()

            try:
                self.update_tasks(writer)
                wait = None
                while self.running or self.tasks:
                    # Opt for efficiency over subsecond timeout accuracy.
                    if wait is None or wait < 1:
                        wait = 1
                    self.iomap.poll(wait)
                    self.update_tasks(writer)
                    wait = self.check_timeout()
            except KeyboardInterrupt:
                # This exception handler tries to clean things up and prints
                # out a nice status message for each interrupted host.
                self.interrupted()

        except KeyboardInterrupt:
            # This exception handler doesn't print out any fancy status
            # information--it just stops.
            pass

        if writer:
            writer.signal_quit()
            writer.join()

        statuses = [task.exitstatus for task in self.done]
        return statuses

    def clear_sigchld_handler(self):
        signal.signal(signal.SIGCHLD, signal.SIG_DFL)

    def set_sigchld_handler(self):
        # TODO: find out whether set_wakeup_fd still works if the default
        # signal handler is used (I'm pretty sure it doesn't work if the
        # signal is ignored).
        signal.signal(signal.SIGCHLD, self.handle_sigchld)
        # This should keep reads and writes from getting EINTR.
        if hasattr(signal, 'siginterrupt'):
            signal.siginterrupt(signal.SIGCHLD, False)

    def handle_sigchld(self, number, frame):
        """Apparently we need a sigchld handler to make set_wakeup_fd work."""
        # Write to the signal pipe (only for Python <2.5, where the
        # set_wakeup_fd method doesn't exist).
        if self.iomap.wakeup_writefd:
            os.write(self.iomap.wakeup_writefd, '\0')
        for task in self.running:
            if task.proc:
                task.proc.poll()
        # Apparently some UNIX systems automatically resent the SIGCHLD
        # handler to SIG_DFL.  Reset it just in case.
        self.set_sigchld_handler()

    def add_task(self, task):
        """Adds a Task to be processed with run()."""
        self.tasks.append(task)

    def update_tasks(self, writer):
        """Reaps tasks and starts as many new ones as allowed."""
        # Mask signals to work around a Python bug:
        #   http://bugs.python.org/issue1068268
        # Since sigprocmask isn't in the stdlib, clear the SIGCHLD handler.
        # Since signals are masked, reap_tasks needs to be called once for
        # each loop.
        keep_running = True
        while keep_running:
            self.clear_sigchld_handler()
            self._start_tasks_once(writer)
            self.set_sigchld_handler()
            keep_running = self.reap_tasks()

    def _start_tasks_once(self, writer):
        """Starts tasks once.

        Due to http://bugs.python.org/issue1068268, signals must be masked
        when this method is called.
        """
        while 0 < len(self.tasks) and len(self.running) < self.limit:
            task = self.tasks.pop(0)
            self.running.append(task)
            task.start(self.taskcount, self.iomap, writer, self.askpass_socket)
            self.taskcount += 1

    def reap_tasks(self):
        """Checks to see if any tasks have terminated.

        After cleaning up, returns the number of tasks that finished.
        """
        still_running = []
        finished_count = 0
        for task in self.running:
            if task.running():
                still_running.append(task)
            else:
                self.finished(task)
                finished_count += 1
        self.running = still_running
        return finished_count

    def check_timeout(self):
        """Kills timed-out processes and returns the lowest time left."""
        if self.timeout <= 0:
            return None

        min_timeleft = None
        for task in self.running:
            timeleft = self.timeout - task.elapsed()
            if timeleft <= 0:
                task.timedout()
                continue
            if min_timeleft is None or timeleft < min_timeleft:
                min_timeleft = timeleft

        if min_timeleft is None:
            return 0
        else:
            return max(0, min_timeleft)

    def interrupted(self):
        """Cleans up after a keyboard interrupt."""
        for task in self.running:
            task.interrupted()
            self.finished(task)

        for task in self.tasks:
            task.cancel()
            self.finished(task)

    def finished(self, task):
        """Marks a task as complete and reports its status to stdout."""
        self.done.append(task)
        n = len(self.done)
        task.report(n)


class IOMap(object):
    """A manager for file descriptors and their associated handlers.

    The poll method dispatches events to the appropriate handlers.
    """
    def __init__(self):
        self.readmap = {}
        self.writemap = {}

        # Setup the wakeup file descriptor to avoid hanging on lost signals.
        wakeup_readfd, wakeup_writefd = os.pipe()
        self.register_read(wakeup_readfd, self.wakeup_handler)
        # TODO: remove test when we stop supporting Python <2.5
        if hasattr(signal, 'set_wakeup_fd'):
            signal.set_wakeup_fd(wakeup_writefd)
            self.wakeup_writefd = None
        else:
            self.wakeup_writefd = wakeup_writefd

    def register_read(self, fd, handler):
        """Registers an IO handler for a file descriptor for reading."""
        self.readmap[fd] = handler

    def register_write(self, fd, handler):
        """Registers an IO handler for a file descriptor for writing."""
        self.writemap[fd] = handler

    def unregister(self, fd):
        """Unregisters the given file descriptor."""
        if fd in self.readmap:
            del self.readmap[fd]
        if fd in self.writemap:
            del self.writemap[fd]

    def poll(self, timeout=None):
        """Performs a poll and dispatches the resulting events."""
        if not self.readmap and not self.writemap:
            return
        rlist = list(self.readmap)
        wlist = list(self.writemap)
        try:
            rlist, wlist, _ = select.select(rlist, wlist, [], timeout)
        except select.error:
            _, e, _ = sys.exc_info()
            errno = e.args[0]
            if errno == EINTR:
                return
            else:
                raise
        for fd in rlist:
            handler = self.readmap[fd]
            handler(fd, self)
        for fd in wlist:
            handler = self.writemap[fd]
            handler(fd, self)

    def wakeup_handler(self, fd, iomap):
        """Handles read events on the signal wakeup pipe.

        This ensures that SIGCHLD signals aren't lost.
        """
        try:
            os.read(fd, READ_SIZE)
        except (OSError, IOError):
            _, e, _ = sys.exc_info()
            errno, message = e.args
            if errno != EINTR:
                sys.stderr.write('Fatal error reading from wakeup pipe: %s\n'
                        % message)
                raise FatalError


class Writer(threading.Thread):
    """Thread that writes to files by processing requests from a Queue.

    Until AIO becomes widely available, it is impossible to make a nonblocking
    write to an ordinary file.  The Writer thread processes all writing to
    ordinary files so that the main thread can work without blocking.
    """
    OPEN = object()
    EOF = object()
    ABORT = object()

    def __init__(self, outdir, errdir):
        threading.Thread.__init__(self)
        # A daemon thread automatically dies if the program is terminated.
        self.setDaemon(True)
        self.queue = queue.Queue()
        self.outdir = outdir
        self.errdir = errdir

        self.host_counts = {}
        self.files = {}

    def run(self):
        while True:
            filename, data = self.queue.get()
            if filename == self.ABORT:
                return

            if data == self.OPEN:
                self.files[filename] = open(filename, 'wb', buffering=1)
                psshutil.set_cloexec(self.files[filename])
            else:
                dest = self.files[filename]
                if data == self.EOF:
                    dest.close()
                else:
                    dest.write(data)

    def open_files(self, host):
        """Called from another thread to create files for stdout and stderr.

        Returns a pair of filenames (outfile, errfile).  These filenames are
        used as handles for future operations.  Either or both may be None if
        outdir or errdir or not set.
        """
        outfile = errfile = None
        if self.outdir or self.errdir:
            count = self.host_counts.get(host, 0)
            self.host_counts[host] = count + 1
            if count:
                filename = "%s.%s" % (host, count)
            else:
                filename = host
            if self.outdir:
                outfile = os.path.join(self.outdir, filename)
                self.queue.put((outfile, self.OPEN))
            if self.errdir:
                errfile = os.path.join(self.errdir, filename)
                self.queue.put((errfile, self.OPEN))
        return outfile, errfile

    def write(self, filename, data):
        """Called from another thread to enqueue a write."""
        self.queue.put((filename, data))

    def close(self, filename):
        """Called from another thread to close the given file."""
        self.queue.put((filename, self.EOF))

    def signal_quit(self):
        """Called from another thread to request the Writer to quit."""
        self.queue.put((self.ABORT, None))

