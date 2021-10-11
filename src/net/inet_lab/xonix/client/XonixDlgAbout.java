package net.inet_lab.xonix.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;

import com.google.gwt.user.client.ui.HTML;

import com.google.gwt.user.client.ui.Image;

import com.google.gwt.user.client.ui.Label;


public class XonixDlgAbout extends DialogBox {
	public XonixDlgAbout () {
		super(false);
		
		setText("About ...");
		setAnimationEnabled(true);
		
		final AbsolutePanel pa_main = new AbsolutePanel();
	
		pa_main.setSize("553px", "308px");
		
		final Image img_xonix = new Image("xonix/xonix128.png");
		pa_main.add(img_xonix);
		
		final Label la_XONIX = new Label("XONIX");
		la_XONIX.setWordWrap(false);
		la_XONIX.setStyleName("gwt-Label-BIG");
		pa_main.add(la_XONIX, 118, -29);
		
		final Label la_copyright = new Label("Copyright (C) 2011 Konstantin Ignatiev");
		pa_main.add(la_copyright, 148, 152);
		
		final HTML html_gws = new HTML("<A href=\"https://chrome.google.com/webstore/detail/laeedgipjndolekchagcmilcmbmbmeem\">Visit our page at Google Web Store</A>", true);
		pa_main.add(html_gws, 158, 176);
		
		final Image img_gwtlogo = new Image("xonix/gwt-logo48.png");
		pa_main.add(img_gwtlogo, 493, 194);
		img_gwtlogo.setSize("50", "50");
		
		final Label la_madewithgwt = new Label("Made with Google Web Toolkit");
		pa_main.add(la_madewithgwt, 310, 226);
		
		final Button bt_dismiss = new Button("Dismiss");
		pa_main.add(bt_dismiss, 220, 250);
		bt_dismiss.setSize("100px", "28px");
		
		bt_dismiss.addClickHandler(new ClickHandler() {
		      public void onClick(ClickEvent event) {
		    	  hide ();
		      }});
		
		this.setWidget(pa_main);
	}
}
