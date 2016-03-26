package mynosql.sm;

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
		return "{\"key\":\"" + key + "\", \"value\":\"" + value + "\" }";
	}
	
};