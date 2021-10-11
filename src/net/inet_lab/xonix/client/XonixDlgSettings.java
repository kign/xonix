package net.inet_lab.xonix.client;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;

public class XonixDlgSettings extends DialogBox {
  final private XonixOptions opts;
  final private Param[] p;
  final private FlexTable flexTable;
  
  final private Button b_reset, b_newgame, b_continue, b_about;
//  private boolean in_the_middle;
  
  private int game_stage;
  static final public int GAME_START=0, GAME_MIDDLE=1, GAME_END=2;
  
  final private HTML html_err = new HTML ("");
  
  public interface DialogActions {
	  public void onClose (boolean is_continue, boolean can_continue, int game_stage);
	  
	  public void onAbout ();
  }
  
  private class Param {
	  final private Label la;
	  final private IntegerBox ib;
	  final private ToggleButton tb;
	  final private XonixOptions.Value v;
	  private Integer v_o;
	  private int v_c;
	  public Param (XonixOptions.Value val, String label) {
		  v = val;
		  la = new Label(label);
		  ib = new IntegerBox ();
		  tb = new ToggleButton ("Default");
		  
		  ib.addChangeHandler(new ChangeHandler() {
				public void onChange(ChangeEvent event) {
					push_values ();
				}
			});
		  tb.addClickHandler(new ClickHandler() {
			  public void onClick(ClickEvent event) {
					push_values ();
					if (tb.isDown())
					  ib.setValue(v.c);
					ib.setEnabled(!tb.isDown());
			  }
		  });
	  }
	  
	  public void insert(FlexTable flexTable, int idx) {
		flexTable.setWidget(idx, 0, la);
		flexTable.setWidget(idx, 1, ib);
		flexTable.setWidget(idx, 2, tb);
		flexTable.getCellFormatter().setHorizontalAlignment(idx, 1, HasHorizontalAlignment.ALIGN_LEFT);
	  }
	  
	  public void initialize () {
		v_o = (v.o == null)? null: v.o.intValue(); // to make sure we have a *copy*
		v_c = v.c;

		ib.setValue((v_o == null)?v_c:v_o);
		ib.setEnabled(v_o != null);
		tb.setDown(v_o == null);
	  }
	  public void push_default () {
		v.o = tb.isDown()? null : ib.getValue();
	  }
	  public void set_default () {
	    if (v.o == null ) ib.setValue(v.c);
	  }
	  public void reset () {
		  v.o = v_o;
	  }
	  public boolean o_changed () {
		  if (v_o == v.o)
			  return false;
		  if (v_o == null || v.o == null)
			  return true;
		  return !v.o.equals(v_o);
	  }
	  public boolean c_changed () {
		  return ib.getValue() != v_c;
	  }
	  
  }

  public XonixDlgSettings (XonixOptions options) {
	super(false);
	opts = options;
	
	setText("Settings");
	setAnimationEnabled(true);
	  
	flexTable = new FlexTable();

	p = new Param[] { 
		  new Param(opts.nX, "Width"),
		  new Param(opts.nY, "Height"),
		  new Param(opts.b, "Border"),
		  new Param(opts.sched, "Redraw time (millisecs.)"),
		  new Param(opts.nticks, "Ticks (must divide 16+Border)"),
		  new Param(opts.given_lives, "Lives"),
		  new Param(opts.n_oo, "# of flying O's"),
		  new Param(opts.new_rat_time, "Rats timeout (secs.)"),
		  new Param(opts.win_per_oo, "Winning threshold per O")	};
	
	for (int idx=0; idx < p.length; idx ++)
		p[idx].insert (flexTable, idx);
	
	flexTable.setWidget(p.length, 0, html_err);
	flexTable.getFlexCellFormatter().setColSpan(p.length, 0, 3);
	
	b_reset = new Button ("Reset");
	b_reset.addClickHandler(new ClickHandler() {
	      public void onClick(ClickEvent event) {
	    	  for (Param par : p)
	    		  par.reset ();
	    	  opts.compute();
	    	  initialize ();
	      }});
	b_newgame = new Button ("*** temp ***");
	b_continue = new Button ("Continue");
	b_about = new Button ("About ...");
		
	this.setWidget(flexTable);
  }

  private void sync_buttons () {
	  boolean o_change = false, c_change = false;
	  for (Param par : p) {
		  o_change |= par.o_changed();
		  c_change |= par.c_changed();
	  }
	
	  b_reset.setEnabled(o_change);
	  b_continue.setEnabled(!c_change);	  
  }
  
  private void push_values () {
	for (Param par : p)
	  par.push_default();
	
    opts.compute();

	for (Param par : p)
	  par.set_default();
	
	sync_buttons ();
  }
  
  private void initialize () {	  
	  for (Param par : p)
		  par.initialize();
	  sync_buttons ();
    // html_err.setHTML("<span style='font-weight:bold; color: red;'>This is a error</span>");
  }
  
  public void init_and_show (int game_stage) {
	  this.game_stage = game_stage;
	  
	  FlexTable bline = new FlexTable();
	  
	  flexTable.setWidget(p.length+1, 0, bline);
	  flexTable.getFlexCellFormatter().setColSpan(p.length+1, 0, 3);
	  
	  if (game_stage == GAME_MIDDLE) {
		  bline.setWidget(0, 0, b_reset);
		  bline.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);
		  bline.setWidget(0, 1, b_newgame);
		  bline.getCellFormatter().setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);
		  bline.setWidget(0, 2, b_continue);
		  bline.getCellFormatter().setHorizontalAlignment(0, 2, HasHorizontalAlignment.ALIGN_RIGHT);
		  b_newgame.setText("New Game");
		  b_newgame.removeStyleDependentName("main");
	  }
	  else {
		  bline.setWidget(0, 0, b_reset);
		  bline.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_LEFT);
		  bline.setWidget(0, 1, b_newgame);
		  bline.getCellFormatter().setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);
		  b_newgame.setText("Play Game !");
		  b_newgame.addStyleDependentName("main");
		  bline.setWidget(0, 2, b_about);
		  bline.getCellFormatter().setHorizontalAlignment(0, 2, HasHorizontalAlignment.ALIGN_RIGHT);

	  }
		
	  initialize ();
	  
	  center ();
  }
  
  public void setDialogActions (final DialogActions act) {
	  b_newgame.addClickHandler(new ClickHandler() {
	      public void onClick(ClickEvent event) {
	    	  push_values ();
	    	  hide ();
	    	  act.onClose(false, b_continue.isEnabled(), game_stage);
	      }});
	  b_continue.addClickHandler(new ClickHandler() {
	      public void onClick(ClickEvent event) {
	    	  push_values ();
	    	  hide ();
	    	  act.onClose(true, b_continue.isEnabled(), game_stage);
	      }});
	  b_about.addClickHandler(new ClickHandler() {
		      public void onClick(ClickEvent event) {
		    	  act.onAbout();
		  }});
  }
}