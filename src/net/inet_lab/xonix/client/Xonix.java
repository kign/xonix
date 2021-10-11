package net.inet_lab.xonix.client;

import java.util.ArrayList;
import java.util.Date;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.ImageLoader;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Xonix implements EntryPoint {

  // game parameters
  private final int cs=16; // determined by images used 16x16, thus can't be changed dynamically
  private int nX, nY, b, nticks;  // taken from opts (just for simplicity)
  private XonixOptions opts;
  private boolean dbg_perf; // output draw performance; use "&perf=" in the URL
  
  // UI elements
  private final FlexTable flexTable;
  private final Label la_header;
  private final MyCanvas canvas;
  private final TextArea ta_debug; // user "&debug=" to enable
  private final HTML txt_rats;
  private final HTML txt_lives;
  private final HTML txt_area;
  private final DlgPause dlg_pause;
  private XonixDlgSettings dlg_settings;
  private XonixDlgAbout dlg_about;
  private final FocusPanel focusPanel;
  
  
  // loaded images
  private final int IMG_XONIX=0, IMG_OO=1, IMG_RAT=2;
  ImageElement imgs[] = new ImageElement[3];
  
  // game objects
  private Obj xonix;
  private Obj[] oo;
  private Obj[] rats = new Obj[20]; // maximum allowed
  private int n_rats;
  private final int BLANK1 = 1;
  private final int BLANK2 = 2;
  private int field[];
  
  // trackers
  private double game_real_time;
  private long time_offset;
  private int game_timer_stop_depth;
  
  private double last_rat_time;
  private int n_lives;
  private final int GS_ACTIVE=1, GS_APP_DEMO=2; 
  private int game_status;
  private int backtrace_progress;
  private final int XM_NORMAL=1, XM_DRAW=2;
  private int xmode;
  private int n_empty;
  private boolean after_collision;
  private ArrayList<Integer> btrace = new ArrayList<Integer>();
  private boolean game_paused;
  private boolean initial_show; // show settings dialog at the start of 1-st game only
  private int rats_counter;
  private boolean request_full_repaint;

  private int tick = 0; 
  
  private Timer timer;
  private final boolean dbg_mode = true;

  private int dbg_t_extra_loops;
  

  public interface MoveAllowed {
	  public boolean allowed(int x, int y);
  }

  private class DlgPause extends DialogBox {
	  private final HorizontalPanel hPanel;
	  private final Button b_resume, b_abort, b_settings;
	  private boolean in_the_middle;
	  public DlgPause () {
		  super(false);
		  setAnimationEnabled(true);
		  
		  hPanel = new HorizontalPanel ();
		  b_resume = new Button ("Resume");
		  b_abort = new Button ("New Game");
		  b_settings = new Button ("Settings...");
			
		  setWidget(hPanel);
			
		  b_resume.addClickHandler(new ClickHandler() {
			      public void onClick(ClickEvent event) {
			    	  hide();
				      resume_game ();
				      }
				    });
		  b_abort.addClickHandler(new ClickHandler() {
			      public void onClick(ClickEvent event) {
			    	  hide (); 
				      GameInitialize();
				      }
				    });
		  b_settings.addClickHandler(new ClickHandler() {
			      public void onClick(ClickEvent event) {
			    	  hide();
			    	  dlg_settings.init_and_show(in_the_middle?XonixDlgSettings.GAME_MIDDLE:XonixDlgSettings.GAME_END);
				      }
				    });
	  }
	  public void display (String msg, boolean in_the_middle) {
		  this.in_the_middle = in_the_middle;
		  
		  hPanel.clear();
		  if (in_the_middle)
			  hPanel.add(b_resume);
		  hPanel.add(b_abort);
		  hPanel.add(b_settings);
		  
			pause_game_w_timer ();
			setText(msg);
			setPopupPositionAndShow(new PopupPanel.PositionCallback() {
				public void setPosition(int offsetWidth, int offsetHeight) {
					setPopupPosition(	
							canvas.getAbsoluteLeft() + canvas.getCoordWidth()/2-offsetWidth/2, 
							canvas.getAbsoluteTop() + canvas.getCoordHeight()/2-offsetHeight/2);
				}
			});	
		  }

	  }
  
  private class Obj {
	  public final ImageElement img;
	  public int x, y, px, py, dx, dy;
	  
	  public Obj(int img_idx, int X, int Y, int dX, int dY) {
		  px = x = X; py = y = Y; dx = dX; dy = dY;
		  img = imgs[img_idx];
	  }	  

	  public void init (int X, int Y, int dX, int dY) {
		  px = x = X; py = y = Y; dx = dX; dy = dY;
	  }	  
      public void moveby(int dx, int dy)
      {
          px = x;
          py = y;
          x += dx;
          y += dy;
      }
      public void moveto(int new_x, int new_y)
      {
          px = x;
          py = y;
          x = new_x;
          y = new_y;
      }
      
      public boolean move_if_can (MoveAllowed allowed) {
          int p_dx = dx, p_dy = dy;
          if (!allowed.allowed(x + dx, y + dy))
          {
              boolean afx = allowed.allowed(x, y + dy);
              boolean afy = allowed.allowed(x + dx, y);

              if (!afx || (afx == afy)) dy *= -1;
              if (!afy || (afx == afy)) dx *= -1;
          }
          if (allowed.allowed(x + dx, y + dy))
          {
              moveby(dx, dy);
              return true;
          }
          else if (((dx != -p_dx) || (dy != -p_dy)) &&
              allowed.allowed(x + (dx + p_dx) / 2, y + (dy + p_dy) / 2))
          {
              moveby((dx + p_dx) / 2, (dy + p_dy) / 2);
              return true;
          }
          else
              return false;
      }

      public boolean isImageLoaded() {
    	  return img != null && _isImageLoaded(img);
      }
      // http://code.google.com/p/google-web-toolkit-incubator/source/browse/trunk/src/com/google/gwt/demos/gwtcanvas/client/LogoDemo.java
      private native boolean _isImageLoaded(ImageElement imgElem) /*-{
        return !!imgElem.__isLoaded;
      }-*/;

  }
  
  private final int KEY_SPACE = 32, KEY_LEFT = 37, KEY_UP = 38, KEY_RIGHT = 39, KEY_DOWN = 40, KEY_Q = 113;
  public class KeysState {
	private boolean[] codes = new boolean[256];
	public KeysState() {
		release ();
	}
	public void onKeyDown(KeyDownEvent event) {
		int code = event.getNativeKeyCode();
		if (code >= 0 && code < codes.length)
			codes[code] = true;
	}
	public void release () {
		for (int ii = 0; ii < codes.length; ii ++)
			codes[ii] = false;
	}
	public void release (int key) {
		codes[key] = false;
	}
	public boolean isKey(int key) {
		return codes[key];
	}
  }
  final KeysState kstate = new KeysState();
  
  public Xonix () {
	  canvas = new MyCanvas();
	  
	  if (Location.getParameter("debug") != null)
		  ta_debug = new TextArea();
	  else
		  ta_debug = null;
	  
	  la_header = new Label("Xonix: " + "(X)" + " x " + "(Y)");
	  
	  txt_lives = new HTML("");
	  txt_rats = new HTML("");
	  txt_area = new HTML("");
	  

	  dlg_pause = new DlgPause();
	  focusPanel = new FocusPanel ();
	  flexTable = new FlexTable();
	  
	  game_paused = true;
	  initial_show = true;	  
	  game_timer_stop_depth = 0;
  }

  public void onModuleLoad() {
	  dbg_perf = Location.getParameter("perf") != null;
	  set_options ();
	  
	  RootPanel rootPanel = RootPanel.get();
	  rootPanel.add(flexTable, 0, 0);
	  
	  la_header.setText("Xonix: " + nX + " x " + nY);
	  
	  flexTable.setWidget(0, 0, la_header);
	  flexTable.getFlexCellFormatter().setColSpan(0, 0, 3);
	  
	  flexTable.setWidget(1, 0, focusPanel);
	  flexTable.getFlexCellFormatter().setColSpan(1, 0, 3);
	  
	  focusPanel.setFocus(true);
	  focusPanel.addKeyDownHandler(new KeyDownHandler() {
		  	public void onKeyDown(KeyDownEvent event) {
		  		kstate.onKeyDown(event);
		  	}
		  });

	  focusPanel.addBlurHandler(new BlurHandler () {
		  public void onBlur(BlurEvent event) {
			  // debug_log ("Blur event!!!");
			  if (!game_paused)
			    user_pause ();
		  }
	  });
	  focusPanel.addFocusHandler(new FocusHandler() {
		  public void onFocus(FocusEvent event) {
			  ; // reserved
		  }
	  });
	  
	  canvas.addMouseDownHandler(new MouseDownHandler() {
	  	public void onMouseDown(MouseDownEvent event) {
	  		; // reserved for future use
	  	}
	  });	  
	    
	  String[] urls = new String[imgs.length]; // {"xonixg.png", "oo.png", "rat.png"};
	  urls[IMG_XONIX] = "xonixg.png";
	  urls[IMG_OO]    = "oo.png";
	  urls[IMG_RAT]   = "rat.png";
	    
	  ImageLoader.loadImages(urls, new ImageLoader.CallBack() {
	      public void onImagesLoaded(ImageElement[] imageHandles) {
		    imgs[IMG_XONIX] = imageHandles[IMG_XONIX];
		    imgs[IMG_OO] = imageHandles[IMG_OO];
		    imgs[IMG_RAT] = imageHandles[IMG_RAT];
	        reportReady (1);
	      }	      
	    });
  
	  focusPanel.add(canvas);
	  
	  flexTable.setWidget(2, 0, txt_rats);
	  flexTable.setWidget(2, 1, txt_lives);
	  flexTable.setWidget(2, 2, txt_area);
	  
	  if (ta_debug != null) {
		  flexTable.setWidget(3, 0, ta_debug);
		  flexTable.getFlexCellFormatter().setColSpan(3, 0, 3);
		  ta_debug.setPixelSize(canvas.getCoordWidth(), 100);
		  ta_debug.setReadOnly(true);
	  }
	  
	  flexTable.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
	  flexTable.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_CENTER);
	  flexTable.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_LEFT);
	  flexTable.getCellFormatter().setHorizontalAlignment(2, 2, HasHorizontalAlignment.ALIGN_RIGHT);
	  flexTable.getCellFormatter().setHorizontalAlignment(2, 1, HasHorizontalAlignment.ALIGN_CENTER);
	  	  
	  timer = new Timer() {
          public void run() {
            timeDispatcher();
          }
        };
            
      debug_log ("w x h: " + Window.getClientWidth() + ", " + Window.getClientHeight());
           
      dlg_settings = new XonixDlgSettings(opts);
      dlg_settings.setDialogActions(new XonixDlgSettings.DialogActions() {	
		@Override
		public void onClose(boolean is_continue, boolean can_continue, int game_stage) {
			dlg_settings_on_close (is_continue, can_continue, game_stage);
		}
			
		public void onAbout () {
			dlg_about.center ();
		}
	});

      dlg_about = new XonixDlgAbout ();
      
      reportReady(0);
  }
  
  private void set_options () {
	opts = new XonixOptions(Cookies.getCookie("xonix-options"));
	opts.set_limits(cs,Window.getClientWidth(),Window.getClientHeight());
	opts.compute();
	nX = opts.nX.c;
	nY = opts.nY.c;
	b = opts.b.c;
	nticks = opts.nticks.c;
	
	int c_x = nX * cs + (nX + 1) * b, c_y = nY * cs + (nY + 1) * b;
	canvas.setCoordSize(c_x, c_y);
	canvas.setSize(c_x + "px", c_y + "px");
	
	field = new int[nX * nY];
  }
  
  private boolean gameready[] = {false, false};
  
  private void reportReady(int idx) {
	assert !gameready[idx];
	gameready[idx] = true;
	for (int ii = 0; ii < gameready.length; ii ++)
		if (!gameready[ii])
			return;
	debug_log ("Ready!");
	GameInitialize ();
  }
  
  private void GameInitialize () {
	xonix = new Obj (IMG_XONIX, nX / 2, 1, 1, 0);
	oo = new Obj[opts.n_oo.c];
    for (int ii = 0; ii < oo.length; ii++)
      oo[ii] = new Obj(IMG_OO, rand_next(2, nX - 2), rand_next(2, nY - 2), 
      				    1 - 2 * rand_next(0, 2), 1 - 2 * rand_next(0, 2));

    n_rats = 1;
    rats[0] = new Obj(IMG_RAT, 1, nY - 1, 1, -1);

    n_lives = opts.given_lives.c;
	  
    for (int y : new int[] { 0, 1, nY - 2, nY - 1 })
      for (int x = 0; x < nX; x++)
    	  field[y*nX + x] = BLANK1;

    for (int x : new int[] {0, 1, nX - 2, nX - 1})
      for (int y = 2; y < nY - 2; y ++)
    	  field[y*nX + x] = BLANK1;
	  
    for (int y = 2; y < nY - 2; y ++)
      for (int x = 2; x < nX - 2; x ++)
    	  field[y*nX + x] = 0;
    
    la_header.setText("Xonix: " + nX + " x " + nY);
    
	tick = nticks;
	  
	after_collision = false;
	dbg_t_extra_loops = 0;
	game_status = GS_ACTIVE;
	backtrace_progress = 0;
	xmode = XM_NORMAL;
	
	request_full_repaint = true;
		
	
	game_paused = false;
	
	n_empty = (nX - 4) * (nY - 4);
	
	reset_game_timer ();
	update_rats_counter (true);
	update_lives_counter ();
	update_area_counter ();
	
	focusPanel.setFocus(true);
	  
	timeDispatcher ();
  }
  
  private void GameUpdate () {
      if (game_status == GS_ACTIVE && backtrace_progress > 0)
      {
          set_game_status(GS_APP_DEMO);
          return;
      }

      if (game_status == GS_APP_DEMO && backtrace_progress <= 1)
      {
          backtrace_progress = 0;
          xmode = XM_NORMAL;
          set_game_status(GS_ACTIVE);

          for (int ii = n_rats - 1; ii >= 0; ii--)
          {
              Obj rat = rats[ii];
              if (field[rat.y * nX + rat.x] == 0)
              {
                  // poor bastard is out of luck, got to be killed
                  for (int jj = ii; jj < n_rats - 1; jj++)
                      rats[jj] = rats[jj + 1];
                  n_rats--;
              }
          }
          // if killed all rats, re-create one
          if (n_rats == 0)
          {
              n_rats = 1;
              rats[0] = new Obj(IMG_RAT, 1, nY - 1, 1, -1);
          }

          // cut user some slack....
          update_rats_counter (true);
          
          xonix.x = btrace.get(0) % nX;
          xonix.y = btrace.get(0) / nX;
          xonix.dx = xonix.dy = 0;

          if (dbg_mode)
          {
              for (int ii = 0; ii < n_rats; ii++)
                  if (field[rats[ii].y * nX  + rats[ii].x] != BLANK1)
                	  dbg_error ( "dead rat remained");
              for (int y = 0; y < nY; y++)
                  for (int x = 0; x < nX; x++)
                      if (field[y * nX +  x] == BLANK2)
                    	  dbg_error ( "backtrace wasn't clean");
          }
          return;
      }

      if (backtrace_progress > 0)
      {
          int bt = backtrace_progress - 1;
          assert xmode == XM_DRAW && game_status == GS_APP_DEMO &&  bt > 0;

          assert field[btrace.get(bt)] == BLANK2;
          
          
          field[btrace.get(bt)] = 0;
          xonix.moveto(btrace.get(bt-1) % nX, btrace.get(bt - 1) / nX);

          backtrace_progress--;
          return;
      }

      if (check_for_collisions())
      {
          life_lost();
          if (xmode == XM_DRAW)
              initiate_backtrace();
          return;
      }

      int xdx = xonix.dx, xdy = xonix.dy;
      
      // we test first directions perpendicular to current movement
      // this way if user clicked two keys in rapid sequence they won't be ignored
      int[] tk = (xdy == 0)? 
    		  new int[] {KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT} :
    		  new int[] {KEY_LEFT, KEY_RIGHT, KEY_UP, KEY_DOWN};
    		  
      /*
      int dbg_cnt = 0;
      for (int key: tk)
    	  if (kstate.isKey(key))
    		  dbg_cnt ++;
      
      if (dbg_cnt > 1)
    	  debug_log ("dbg_cnt = " + dbg_cnt);
      */
      
      int pkey = -1;
      for (int key : tk)
    	  if (pkey < 0 && kstate.isKey(key)) {
    		  kstate.release(key);
    		  pkey = key;
    	  }
    	  
      if (pkey == KEY_LEFT) { xdx = -1; xdy = 0; }
      else if (pkey == KEY_RIGHT) { xdx = +1; xdy = 0; }
      else if (pkey == KEY_UP) { xdx = 0; xdy = -1; }
      else if (pkey == KEY_DOWN) { xdx = 0; xdy = +1; }

      // prevent direction change from having any effect if
      // (a) it would move xonix outside of screen boundaries
      //     (so that user could not use it for a "quick stop" workaround - 
      //      it ain't *that* easy to stop xonix!),  or
      // (b) if it would change direction to the opposite one in "DRAW" mode - 
      //      this would immediate end the game, so user can only do it by mistake
      // (c) it would *immediately* push xonix into one of "forbidden" cells -
      //      again, user would only do that by mistake
      if ((xdx != xonix.dx || xdy != xonix.dy)
                                                                      &&
          !(xmode == XM_DRAW && xdx == -xonix.dx && xdy == -xonix.dy) 
                                                                      &&
          xonix.x + xdx >= 0 && xonix.x + xdx < nX &&
          xonix.y + xdy >= 0 && xonix.y + xdy < nY
                                                                      &&
          !(field[(xonix.y + xdy) * nX + (xonix.x + xdx)] == BLANK2) )
      {
          xonix.dx = xdx;
          xonix.dy = xdy;
      }

      if (xonix.x + xonix.dx >= 0 && xonix.x + xonix.dx < nX &&
          xonix.y + xonix.dy >= 0 && xonix.y + xonix.dy < nY &&
          (xonix.dx != 0 || xonix.dy != 0))
      {
          int xpx = xonix.x, xpy = xonix.y;
          if (xmode == XM_DRAW)
          {
              btrace.add(xpx + xpy * nX);
              field[xonix.y * nX + xonix.x] = BLANK2;
          }
          xonix.moveby(xonix.dx, xonix.dy);
          if (xmode == XM_NORMAL &&
              field[xonix.y * nX + xonix.x] == 0)
          {
              xmode = XM_DRAW;
              btrace.clear();
              btrace.add(xpx + nX * xpy);
          }

          else if (xmode == XM_DRAW &&
              field[xonix.y * nX + xonix.x] == BLANK1)
          {
              xmode = XM_NORMAL;
              xonix.dx = xonix.dy = 0;

              update_rats_counter (true);
              
              if (n_rats > 1)
              {
                  // One random rat remains
                  int ir = rand_next(0, n_rats);
                  if (ir != 0)
                  {
                      Obj r = rats[ir];
                      rats[ir] = rats[0];
                      rats[0] = r;
                  }
                  n_rats = 1;
              }

              paint_empty();
              
              update_area_counter ();
              request_full_repaint = true;

              if (n_empty <= opts.win_per_oo.c * oo.length)
              {
                  dlg_pause.display("*** YOU WON ***", false);
                  return;
              }

          }
      }


      for (Obj o : oo)
      {
          if (field[(o.y + o.dy) * nX + (o.x + o.dx)] == BLANK2)
          {
              life_lost ();
              initiate_backtrace();
              return;
          }

          o.move_if_can(new MoveAllowed() {
	            public boolean allowed(int x, int y) {
		            return field[y * nX + x] == 0;
	            }});
      }
     

      if (opts.new_rat_time.c < (game_real_time - last_rat_time))
      {
          if (n_rats < rats.length)
          {
              rats[n_rats] = new Obj(IMG_RAT, 1, nY - 1, 1, -1);
              n_rats++;
          }
          update_rats_counter (true);      }

      for (int ii = 0; ii < n_rats; ii++) {
    	  Obj rat = rats[ii];
    	  rat.move_if_can(new MoveAllowed() {
        	  public boolean allowed(int x, int y) {
        		  return x >= 0 && x < nX && y >= 0 && y < nY && (field[y * nX + x] == BLANK1 || field[y * nX + x] == BLANK2);
        	  }});
      }     
  }
  
  private boolean check_for_collisions ()  {
      if (after_collision)
          // we already lost life because of collision, now have to move on
          return (after_collision = false);

      if (field[xonix.y * nX + xonix.x] == BLANK2)
      {
          assert xmode == XM_DRAW;
          return true;
      }

      for (int ii = 0; ii < n_rats; ii++)
          if (rats[ii].x == xonix.x && rats[ii].y == xonix.y)
              return (after_collision = true);

      return false;
  }

  private boolean life_lost() {
      n_lives--;
      update_lives_counter ();
      if (n_lives <= 0) {
    	  dlg_pause.display("YOU LOST!", false);
          return true;
      }
      else {
    	  dlg_pause.display("LIFE LOST!", true);
          return false;
      }
  }

  private void initiate_backtrace()
  {
      assert btrace.size() > 0 && xmode == XM_DRAW;
      backtrace_progress = btrace.size();
  }

  private void paint_empty()
  {
      final int FILL = 100;

      for (Obj o : oo)
      {
          int x, y0, y1, x0, x1;
          for (x0 = o.x; field[o.y * nX + x0] == 0; x0--) ; x0++;
          for (x1 = o.x; field[o.y * nX + x1] == 0; x1++) ; x1--;
          for (x = x0; x <= x1; x++)
          {
              if (field[o.y * nX + x] != FILL)
              {
                  for (y0 = o.y; field[y0 * nX + x] == 0; y0--) field[y0 * nX + x] = FILL; y0++;
                  for (y1 = o.y; field[y1 * nX + x] == 0; y1++) field[y1 * nX + x] = FILL; y1--;
              }
          }
      }

      boolean done;
      int extra_loops = -1;
      do
      {
          done = true;
          extra_loops++;
          for (int y = 2; y < nY - 2; y++)
          {
              int x = 1;
              while (x < nX - 2)
              {
                  int x1;
                  for (; field[y * nX + x] != 0 && x < nX - 2; x++) ;
                  if (x < nX - 2)
                  {
                      for (x1 = x; field[y * nX + x1] == 0; x1++) ;
                      if (field[y * nX + x - 1] == FILL || field[y * nX + x1] == FILL)
                      {
                          done = false;
                          for (int xv = x; xv < x1; xv++) field[y * nX + xv] = FILL;
                      }
                      x = x1;
                  }
              }
          }
          for (int x = 2; x < nX - 2; x++)
          {
              int y = 1;
              while (y < nY - 2)
              {
                  int y1;
                  for (; field[y * nX + x] != 0 && y < nY - 2; y++) ;
                  if (y < nY - 2)
                  {
                      for (y1 = y; field[y1 * nX + x] == 0; y1++) ;
                      if (field[(y - 1) * nX + x] == FILL || field[y1 * nX + x] == FILL)
                      {
                          done = false;
                          for (int yv = y; yv < y1; yv++) field[yv * nX + x] = FILL;
                      }
                      y = y1;
                  }
              }
          }
      }
      while (!done);

      dbg_t_extra_loops += extra_loops;

      if (dbg_mode)
      {
          for (int y = 0; y < nY; y++)
              for (int x = 0; x < nX; x++)
                  if (field[y * nX + x] == FILL &&
                      (field[(y - 1) * nX + x] == 0 || field[(y + 1) * nX + x] == 0 || field[y * nX + x - 1] == 0 || field[y * nX + x + 1] == 0))
                      assert false; // ("not properly replaced");
      }

      n_empty = 0;
      for (int y = 2; y < nY - 2; y++)
          for (int x = 2; x < nX - 2; x++)
          {
              int f = field[y * nX + x];
              if (f == FILL)
              {
            	  field[y * nX + x] = 0;
                  // fassign(x, y, 0);
                  n_empty++;
              }
              else if (f == 0 || f == BLANK2) field[y * nX + x] = BLANK1; // fassign(x, y, BLANK1); // field[y, x] = BLANK1;
          }

      
      
      if (dbg_mode)
      {
          for (int y = 0; y < nY; y++)
              for (int x = 0; x < nX; x++)
                  if (field[y * nX + x] == BLANK2 || field[y * nX + x] == FILL) 
                	  dbg_error ("BLANK2 or FILL should have been replaced");
      }
  }
  
  private void user_pause () {
	dlg_pause.display ("Game paused", true);
  }
  
  private void resume_game () {
	  resume_game_w_timer ();
	  focusPanel.setFocus(true);
	  timeDispatcher ();
  }
  
  private void set_game_status(int new_status)
  {
      game_status = new_status;
  }

  int x0, x1, y0, y1;
  private void obj_outline (Obj obj, boolean reset) {
	  if (reset){
		  x0 = nX;
		  x1 = 0;
		  y0 = nY;
		  y1 = 0;
	  }
	  if (obj.x < x0)
		  x0 = obj.x;
	  if (obj.x >= x1)
		  x1 = obj.x + 1;
	  if (obj.y < y0)
		  y0 = obj.y;
	  if (obj.y >= y1)
		  y1 = obj.y + 1;
	  
	  if (obj.px < x0)
		  x0 = obj.px;
	  if (obj.px >= x1)
		  x1 = obj.px + 1;
	  if (obj.py < y0)
		  y0 = obj.py;
	  if (obj.py >= y1)
		  y1 = obj.py + 1;
  }
  
  private void GameDraw () {
	  double t0 = Duration.currentTimeMillis();
	  boolean global_repaint = false;
	  
	  if (global_repaint) {
		  obj_outline(xonix,true);
		  for (Obj o : oo)
			  obj_outline(o,false);
		  for (int ii = 0; ii < n_rats; ii ++)
			  obj_outline(rats[ii],false);

		  repaint(x0,y0,x1,y1);
	  }
	  else {
		  obj_outline(xonix,true);
		  repaint(x0,y0,x1,y1);
		  for (Obj o : oo) {
			  obj_outline(o,true);
			  repaint(x0,y0,x1,y1);
		  }
		  for (int ii = 0; ii < n_rats; ii ++) {
			  obj_outline(rats[ii],true);
			  repaint(x0,y0,x1,y1);
		  }		  
	  }
	  
	  draw_object(xonix);
	  for (Obj o : oo)
		  draw_object(o);
	  for (int ii = 0; ii < n_rats; ii ++)
		  draw_object(rats[ii]);
	  
	  dbg_registerDrawPerformance((int)(Duration.currentTimeMillis() - t0));
  }

  private void draw_object(Obj obj) {
	  int dx = (obj.x - obj.px) * (cs + b) * (nticks - tick)/nticks;
	  int dy = (obj.y - obj.py) * (cs + b) * (nticks - tick)/nticks;
	  	  
	  canvas.drawImage(obj.img, obj.px * cs + (obj.px + 1) * b + dx, 
              obj.py * cs + (obj.py + 1) * b + dy);
	  
	  if (tick == 0) {
		  obj.px = obj.x;
		  obj.py = obj.y;
	  }
	  
  }
  
  private void dlg_settings_on_close (boolean is_continue, boolean can_continue, int game_stage) {
	  initial_show = false;
	  focusPanel.setFocus(true);
	  
	  if (!is_continue) {
		  String dec = opts.to_base64str();
		  Cookies.setCookie("xonix-options", dec);
	  }
		
	  if (!can_continue) {
		nX = opts.nX.c;
		nY = opts.nY.c;
		b = opts.b.c;
		nticks = opts.nticks.c;

		int c_x = nX * cs + (nX + 1) * b, c_y = nY * cs + (nY + 1) * b;
		
		canvas.setCoordSize(c_x, c_y);
		canvas.setSize(c_x + "px", c_y + "px");
		
		field = new int[nX * nY];
		
		GameInitialize ();
	  }
	  else if (game_stage == XonixDlgSettings.GAME_START || is_continue) {
		  resume_game_w_timer ();
		  timeDispatcher ();
	  }
	  else {
		  GameInitialize ();
	  }
  }
  
  private void timeDispatcher () {	  
	  tick --;

	  if (game_timer_stop_depth == 0)
		  game_real_time = ((new Date()).getTime() - time_offset)/1000.0;
	  
	  GameDraw ();	
	  update_rats_counter (false);
	  if (tick == 0) {
		  GameUpdate ();
		  // txt_lives.setHTML("time " + game_real_time);
		  tick = nticks;
	  }
	  
	  if (kstate.isKey(KEY_SPACE) || kstate.isKey(KEY_Q)) {
		  user_pause ();
		  kstate.release();
	  }
	  /*
	  else if (tick == nticks)
		  kstate.release();
	  */
	  if (initial_show) {
		  pause_game_w_timer ();
		  dlg_settings.init_and_show(XonixDlgSettings.GAME_START);
	  }
	  
	  if (!game_paused)
		  timer.schedule(opts.sched.c);
  }
  
  private int rand_next (int a, int b) {
	  return a + Random.nextInt(b-a);
  }
  
  private int dbg_log_cnt = 0;
  private void debug_log (String msg) {
	  if (ta_debug != null) {
		  dbg_log_cnt ++;
		  if (dbg_log_cnt < 100)
			  ta_debug.setText(ta_debug.getText() + msg + "\n");
		  else if (dbg_log_cnt == 100)
			  ta_debug.setText(ta_debug.getText() + "---- Debug log truncated ----" + "\n");
	  }		  
  }
  
  private void update_lives_counter () {
	  txt_lives.setHTML("Lives: <b>" + n_lives + "</b>");
  }
  private void update_area_counter () {
	  txt_area.setHTML("<b>" + n_empty + "</b> (<i>" + (opts.win_per_oo.c * oo.length) + "</i>)");
  }
  private void update_rats_counter (boolean reset) {
	  if (reset){
		  last_rat_time = game_real_time;
		  rats_counter = 0;
	  } 	  
	  int x = opts.new_rat_time.c - (int)(game_real_time - last_rat_time);
	  if (x != rats_counter) {
		  rats_counter = x;
		  txt_rats.setHTML("Rats: <b>" + rats_counter + "</b>");
	  }
  }
  
  private void repaint (int x0, int y0, int x1, int y1) {
	  canvas.setFillStyle(new Color(0x64, 0x95, 0xED)); // CornflowerBlue
	  
	  if (request_full_repaint) {
		  request_full_repaint  = false;
		  x0 = 0;
		  y0 = 0;
		  x1 = nX;
		  y1 = nY;
	  }
	  
	  canvas.fillRect(
			  x0 * cs + x0 * b,       y0 * cs + y0 * b,
			  (x1-x0) * cs + (x1-x0 + 1) * b, (y1-y0) * cs + (y1-y0 + 1) * b);

	  for (int iy = y0; iy < y1; iy ++)
		  for (int ix = x0; ix < x1; ix ++) {
			  int f = field[iy*nX + ix];
			  if (f != 0)
				  drawBackgroundElm (f == BLANK2, ix, iy);
		  }
		  
  }
  
  private void pause_game_w_timer () {
	  if (!game_paused) {
		  game_paused = true;
		  stop_game_timer ();
	  }
  }
  
  private void resume_game_w_timer () {
	  if (game_paused) {
		  game_paused = false;
		  start_game_timer ();
	  }
  }
  private void stop_game_timer () {
	  game_timer_stop_depth ++;
  }
  private void start_game_timer () {
	  game_timer_stop_depth --;
	  if (game_timer_stop_depth == 0)
		  time_offset = new Date().getTime() - (long)(1000 * game_real_time);
  }
  private void reset_game_timer () {
	  time_offset = (new Date()).getTime();
	  game_timer_stop_depth = 0;
	  game_real_time = 0.0;
  }
  
  private void drawBackgroundElm (boolean is_temp, int x, int y) {
	  Color fc = is_temp? new Color(240,240,240) : new Color(200,200,200);
	  canvas.setFillStyle(fc);
	  canvas.fillRect(x * cs + (x + 1) * b, y * cs + (y + 1) * b, cs, cs);
	  
  }
  
  private void dbg_error(String msg) {
	  debug_log( "ERROR: " + msg);
  }
  
  private int dbgdp_cnt = 0;
  private int dbgdp_tot = 0;
  private void dbg_registerDrawPerformance(int dt) {
	  dbgdp_tot += dt;
	  dbgdp_cnt += 1;
	  
	  if(dbgdp_cnt == 100) {
		  // debug_log ("Time: " + game_real_time);

		  if (dbg_perf)
			  txt_area.setHTML("perf " + ((double)dbgdp_tot/dbgdp_cnt));
		  dbgdp_tot = dbgdp_cnt = 0;
	  }
  }
}
