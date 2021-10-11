package net.inet_lab.xonix.client;

import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

// code borrowed from http://code.google.com/p/google-web-toolkit-incubator/wiki/GWTCanvas

public class MyCanvas extends GWTCanvas implements HasMouseDownHandlers, HasKeyDownHandlers {

	  public MyCanvas () {
		  super ();
	  }
	  
	  public MyCanvas(int coordX, int coordY) {
		  super(coordX, coordY);
	  }

	  public MyCanvas(int coordX, int coordY, int pixelX, int pixelY) {
		  super(coordX, coordY, pixelX, pixelY);
	  }


	@Override
	public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
		return addDomHandler(handler, MouseDownEvent.getType());
	}
	
	// does not have much sense, canvas isn't focusable
	// use FocusPanel instead
	public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
		return addDomHandler(handler, KeyDownEvent.getType());
	}
}
