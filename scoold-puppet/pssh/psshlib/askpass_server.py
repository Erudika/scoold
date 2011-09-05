#!/usr/bin/env python
# -*- Mode: python -*-

# Copyright (c) 2009, Andrew McNabb

"""Sends the password over a socket to askpass.
"""

import errno
import getpass
import os
import socket
import sys
import tempfile
import textwrap

from psshlib import psshutil


class PasswordServer(object):
    """Listens on a UNIX domain socket for password requests."""
    def __init__(self):
        self.sock = None
        self.tempdir = None
        self.address = None
        self.socketmap = {}
        self.buffermap = {}

    def start(self, iomap, backlog):
        """Prompts for the password, creates a socket, and starts listening.

        The specified backlog should be the max number of clients connecting
        at once.
        """
        message = ('Warning: do not enter your password if anyone else has'
                ' superuser privileges or access to your account.')
        print(textwrap.fill(message))

        self.password = getpass.getpass()

        # Note that according to the docs for mkdtemp, "The directory is
        # readable, writable, and searchable only by the creating user."
        self.tempdir = tempfile.mkdtemp(prefix='pssh.')
        self.address = os.path.join(self.tempdir, 'pssh_askpass_socket')
        self.sock = socket.socket(socket.AF_UNIX)
        psshutil.set_cloexec(self.sock)
        self.sock.bind(self.address)
        self.sock.listen(backlog)
        iomap.register_read(self.sock.fileno(), self.handle_listen)

    def handle_listen(self, fd, iomap):
        try:
            conn = self.sock.accept()[0]
        except socket.error:
            _, e, _ = sys.exc_info()
            number = e.args[0]
            if number == errno.EINTR:
                return
            else:
                # TODO: print an error message here?
                self.sock.close()
                self.sock = None
        fd = conn.fileno()
        iomap.register_write(fd, self.handle_write)
        self.socketmap[fd] = conn
        self.buffermap[fd] = self.password

    def handle_write(self, fd, iomap):
        buffer = self.buffermap[fd]
        conn = self.socketmap[fd]
        try:
            bytes_written = conn.send(buffer)
        except socket.error:
            _, e, _ = sys.exc_info()
            number = e.args[0]
            if number == errno.EINTR:
                return
            else:
                self.close_socket(fd, iomap)

        buffer = buffer[bytes_written:]
        if buffer:
            self.buffermap[fd] = buffer
        else:
            self.close_socket(fd, iomap)

    def close_socket(self, fd, iomap):
        iomap.unregister(fd)
        self.socketmap[fd].close()
        del self.socketmap[fd]
        del self.buffermap[fd]

    def __del__(self):
        if self.sock:
            self.sock.close()
            self.sock = None
        if self.address:
            os.remove(self.address)
        if self.tempdir:
            os.rmdir(self.tempdir)

