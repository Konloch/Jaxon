package kernel.linux;

public class XButtonEvent extends STRUCT
{
	int type; /* has to be ButtonPress */
	int serial; /* # of last request processed by server */
	int send_event;  /* true if this came from a SendEvent request */
	int display;  /* Display the event was read from */
	int window;          /* "event" window it is reported relative to */
	int root;          /* root window that the event occurred on */
	int subwindow;  /* child window */
	int time;    /* milliseconds */
	int x, y;    /* pointer x, y coordinates in event window */
	int x_root, y_root;  /* coordinates relative to root */
	int state;  /* key or button mask */
	int button;  /* detail */
	int same_screen;  /* same screen flag */
}