package net.inet_lab.xonix.client;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public class XonixOptions {
  public Value nX, nY, b, sched, nticks, given_lives, n_oo, new_rat_time, win_per_oo;
  private int width, height, cs;
  
  public class Value {
	public final String name;
	public Integer o; // original value;
	public int c; // computed value
	
	public Value(String opt_name, JSONObject jobj) {
	  name = opt_name;
	  if (jobj == null || jobj.get(name) == null || jobj.get(name).isNumber() == null)
		o = null;
	  else
		o = (int)jobj.get(name).isNumber().doubleValue();		  
	}
	public void compute(int def_v, int min_v, int max_v) {
      c = (o == null)?def_v:o;
      if (c > max_v) c = max_v;
      if (c < min_v) c = min_v;
	}
	public JSONValue toJSON () {
	  if (o == null)
		return null;
	  return new JSONNumber(o);
	}
  }
  
  public void set_limits (int cs, int width, int height) {
	this.cs = cs;
	this.width = width;
	this.height = height;
  }

  public void compute () {
	b.compute(4,1,10);
	int max_x = (width - 40 - b.c)/(cs + b.c);
	int max_y = (height - 100 - b.c)/(cs + b.c);
	
	int def_y = 9 * max_x / 16;
	if (def_y < 25)
		def_y = max_y;
	nX.compute (max_x,10,max_x);
	nY.compute (def_y,10,max_y);
	
	sched.compute(20, 1, 10000);
	int nt = 10;
	for (; nt >= 1 && (cs + b.c) % nt > 0; nt --);
	
	nticks.compute(nt, 1, 50);
	if ((cs + b.c) % nticks.c > 0)
	  nticks.c = nt;
	
	given_lives.compute(3, 1, 99);
	int def_oo = (nX.c - 4) * (nY.c - 4)/190;
	if (def_oo < 3) 
		def_oo = 3;
	n_oo.compute(def_oo, 1, 50+def_oo);
	new_rat_time.compute(20, 2, 100);
	win_per_oo.compute(20, 15, 40);
  }
  
  public XonixOptions () {
	this((JSONObject)null);
  }
  private static JSONObject stringbase64_to_json (String b64_json) {
	if (b64_json == null || b64_json.equals(""))
	  return null;
	else {
		String dec = GWT_Base64.decode(b64_json);
		if (dec == null || dec.equals(""))
			return null;
		JSONValue val = JSONParser.parseStrict(dec);
		if (val == null || val.isObject() == null)
			return null;
		return val.isObject();				
	}
  }
  public XonixOptions (String b64_json) {
	this(stringbase64_to_json(b64_json));
  }
  public XonixOptions (JSONObject jobj) {
	nX           = new Value ("nX", jobj);
	nY           = new Value ("nY", jobj);
	b            = new Value ("b", jobj);
	sched        = new Value ("sched", jobj);
	nticks       = new Value ("nticks", jobj);
	given_lives  = new Value ("given_lives", jobj);
	n_oo         = new Value ("n_oo", jobj);
	new_rat_time = new Value ("new_rat_time", jobj);
	win_per_oo   = new Value ("win_per_oo", jobj);
  }
   
  public JSONObject toJSON() {
	JSONObject jobj = new JSONObject();
	for (Value v : new Value[]{nX,nY,b,sched, nticks, given_lives, n_oo, new_rat_time, win_per_oo})
	  if (v.o != null)
	    jobj.put(v.name, v.toJSON());
	return jobj;
  }
  public String to_base64str () {
	return GWT_Base64.encode(toJSON().toString());
  }
}

