///*-
// * ========================LICENSE_START=================================
// * io.openslice.portal.api
// * %%
// * Copyright (C) 2019 openslice.io
// * %%
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * 
// *      http://www.apache.org/licenses/LICENSE-2.0
// * 
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * =========================LICENSE_END==================================
// */
//
//
//package io.openslice.mano;
//import java.util.UUID;
//import portal.api.bus.BusController;
//
///**
// * @author ctranoris
// *
// */
//public class MANOStatus
//{
//	//1: Active, 0:Failed
//	private static Status osm4CommunicationStatus = Status.Active;
//	private static String osm4CommunicationStatusUUID = null;	
//	private static Status osm5CommunicationStatus = Status.Active;
//	private static String osm5CommunicationStatusUUID = null;	
//	private static String message;
//	public static enum Status {Failed,Active};
//	private static java.util.concurrent.locks.ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();
//	
//	public static Status getOsm5CommunicationStatus() {
//		lock.readLock().lock();
//        try {		
//        	return osm5CommunicationStatus;
//        } finally {
//            lock.readLock().unlock();
//        }
//	}
//
//	public static void setOsm5CommunicationStatus(Status osm5CommunicationStatus) {
//        lock.writeLock().lock();
//        try {		
//    		MANOStatus.osm5CommunicationStatus = osm5CommunicationStatus;
//        } finally {
//            lock.writeLock().unlock();
//        }
//	}
//	
//	public static String getOsm5CommunicationStatusUUID() {
//		lock.readLock().lock();
//        try {		
//        	return osm5CommunicationStatusUUID;
//        } finally {
//            lock.readLock().unlock();
//        }
//	}
//
//	public static void setOsm5CommunicationStatusUUID(String osm5CommunicationStatusUUID) {
//        lock.writeLock().lock();
//        try {		
//        	MANOStatus.osm5CommunicationStatusUUID = osm5CommunicationStatusUUID;
//        } finally {
//            lock.writeLock().unlock();
//        }
//	}
//
//	public static String getMessage() {
//		lock.readLock().lock();
//        try {		
//        	return message;
//        } finally {
//            lock.readLock().unlock();
//        }
//	}
//
//	public static void setMessage(String message) {
//        lock.writeLock().lock();
//        try {		
//        	MANOStatus.message = message;
//        } finally {
//            lock.writeLock().unlock();
//        }
//	}
//
//	
//	
//
//	
//	public static void setOsm5CommunicationStatusFailed(String message) {
//		lock.writeLock().lock();
//		try {		
//			if(message == null)
//			{
//				message="";
//			}
//			if(MANOStatus.osm5CommunicationStatus == Status.Active)
//			{			
//				MANOStatus.osm5CommunicationStatus = Status.Failed ;
//				MANOStatus.setMessage("OSM5 communication failed." + message);
//				MANOStatus.setOsm5CommunicationStatusUUID(UUID.randomUUID().toString());
//				System.out.println("Inside setOSM5CommunicationStatusFailed. "+MANOStatus.getOsm5CommunicationStatusUUID().toString()+","+MANOStatus.getMessage().toString());
//				BusController.getInstance().osm5CommunicationFailed(MANOStatus.class);					
//			}
//	    } finally {
//	        lock.writeLock().unlock();
//	    }		
//	}
//	
//	
//
//	public static void setOsm5CommunicationStatusActive(String message) {
//		lock.writeLock().lock();
//		try {		
//			// TODO Auto-generated method stub
//			if(message == null)
//			{
//				message="";
//			}
//			if(MANOStatus.osm5CommunicationStatus == Status.Failed)
//			{
//				MANOStatus.osm5CommunicationStatus = Status.Active ;
//				MANOStatus.setMessage("OSM5 communication restored." + message);
//				BusController.getInstance().osm5CommunicationRestored(MANOStatus.class);					
//			}		
//	    } finally {
//	        lock.writeLock().unlock();
//	    }				
//	}
//}
//
