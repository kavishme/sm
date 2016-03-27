package mynosql.sm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class KeyValue{
	public String key, value;
	
	public KeyValue(String k, String v)
	{
		key = k;
		value = v;
	}
	
	public String ToJSONString()
	{
		String str = "{\"key\":\"" + key + "\", \"value\":\"" + value + "\" }";
		try {
			str = URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}
	
};