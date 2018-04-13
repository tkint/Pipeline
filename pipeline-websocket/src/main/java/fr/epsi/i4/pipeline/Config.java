package fr.epsi.i4.pipeline;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

	private final InputStream inputStream;

	private final String propertiesFileName;

	private final Properties properties;

	public Config(String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
		this.inputStream = this.getClass().getClassLoader().getResourceAsStream(propertiesFileName);
		this.properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public Properties getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public Integer getIntegerProperty(String key) {
		return Integer.valueOf(getProperty(key));
	}
}
