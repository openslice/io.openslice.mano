package io.openslice.mano;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
public class AlarmsService {

	private static final transient Log logger = LogFactory.getLog(AlarmsService.class.getName());
	public void createAlarm(String body) {
			
		logger.info("createAlarm body = " + body);
		
		
	}
}
