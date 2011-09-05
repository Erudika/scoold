# Copyright (c) 2009, Andrew McNabb
# Copyright (c) 2003-2008, Brent N. Chun

def with_color(string, fg, bg=49):
    '''Given foreground/background ANSI color codes, return a string that,
    when printed, will format the supplied string using the supplied colors.
    '''
    return "\x1b[%dm\x1b[%dm%s\x1b[39m\x1b[49m" % (fg, bg, string)

def B(string):
    '''Returns a string that, when printed, will display the supplied string
    in ANSI bold.
    '''
    return "\x1b[1m%s\x1b[22m" % string

def r(string): return with_color(string, 31) # Red
def g(string): return with_color(string, 32) # Green
def y(string): return with_color(string, 33) # Yellow
def b(string): return with_color(string, 34) # Blue
def m(string): return with_color(string, 35) # Magenta
def c(string): return with_color(string, 36) # Cyan
def w(string): return with_color(string, 37) # White

#following from Python cookbook, #475186
def has_colors(stream):
    '''Returns boolean indicating whether or not the supplied stream supports
    ANSI color.
    '''
    if not hasattr(stream, "isatty"):
        return False
    if not stream.isatty():
        return False # auto color only on TTYs
    try:
        import curses
        curses.setupterm()
        return curses.tigetnum("colors") > 2
    except:
        # guess false in case of error
        return False
