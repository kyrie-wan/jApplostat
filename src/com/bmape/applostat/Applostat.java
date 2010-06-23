/**
 * 
 */
package com.bmape.applostat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import net.sourceforge.prowl.api.DefaultProwlEvent;
import net.sourceforge.prowl.api.ProwlClient;
import net.sourceforge.prowl.api.ProwlEvent;
import net.sourceforge.prowl.exception.ProwlException;

import com.bmape.applostat.util.UrlConnection;

/**
 * 
 * @author martinoswald
 */
public class Applostat {
	private static final String DELIVERY_DATE = "delivery_date";
	private static final String SHIPPING_DATE = "shipping_date";
	private static final String ORDER_STATUS = "order_status";
	private String orderNumber;
	private String emailAddress;
	private String itemName;
	private String prowlApiKey;

	public Applostat(String orderNumber, String emailAddress, String itemName, String prowlApiKey) {
		super();
		this.orderNumber = orderNumber;
		this.emailAddress = emailAddress;
		this.itemName = itemName;
		this.prowlApiKey = prowlApiKey;
	}

	public static void main(String[] args) {
		
		if (args.length != 4) {
			System.out.println("OrderStatus <order number> <email address> <item name> <prowl api-key>");
			return;
		}
		
		Applostat order = new Applostat(args[0], args[1], args[2], args[3]);
		
		System.out.println("Start retrieving order status ...");
		String pageContent = order.retrieveOrderPage();
		System.out.println("Locating order status ...");
		String orderStatus = order.getOrderStatus(pageContent);
		System.out.println("OrderStatus: " + orderStatus);
		System.out.println("Locating order dates ...");
		String[] orderDates = order.getOrderDates(pageContent);
		String shippingDate = orderDates[0].trim();
		String deliveryDate = orderDates[1].trim();
		System.out.println("Shipping-Date: " + shippingDate);
		System.out.println("Delivery-Date: " + deliveryDate);
		System.out.println("Check if order details changed ...");
		order.sendNotification(orderStatus, shippingDate, deliveryDate);
		System.out.println("Finished");
		
	}
	
	public void sendNotification(String orderStatus, String shippingDate, String deliveryDate) {
		
		String message = "Status: " + orderStatus + "\n" + shippingDate + "\n" + deliveryDate;
		
		boolean sendNotifaction = false;
		Properties properties = this.loadOrderStatus();
		if (properties.size() != 3) {
			this.saveOrderStatus(orderStatus, shippingDate, deliveryDate);
			sendNotifaction = true;
			
		} else {
			
			String propOrderStatus = properties.getProperty(ORDER_STATUS);
			String propShippingDate = properties.getProperty(SHIPPING_DATE);
			String propDeliveryDate = properties.getProperty(DELIVERY_DATE);
			
			if (propOrderStatus == null || !propOrderStatus.equals(orderStatus)) {
				System.out.println("Order status changed");
				message = "Order status changed\n" + message;
				sendNotifaction = true;
				
			} 
			if (propShippingDate == null || !propShippingDate.equals(shippingDate)) {
				System.out.println("Shipping date changed");
				message = "Shipping date changed\n" + message;
				sendNotifaction = true;
				
			} 
			if (propDeliveryDate == null || !propDeliveryDate.equals(deliveryDate)) {
				System.out.println("Delivery date changed");
				message = "Delivery date changed\n" + message;
				sendNotifaction = true;
			}
		}
		
		if (sendNotifaction) {
			System.out.println("Sending prowl notifaction ...");
			String response = this.sendProwlNotifcation(message);
			System.out.println(response);
		} else {
			System.out.println("Nothing changed");
		}
	}
	
	private String getOrderHttpUrl() {
		StringBuffer url = new StringBuffer();
		
		url.append("http://store.apple.com/go/gb/e/vieworder/");
		url.append(this.orderNumber);
		url.append("/");
		url.append(this.emailAddress);
		url.append("/AOSA10000063608");
		
		return url.toString();
	}

	private String sendProwlNotifcation(String message) {
		ProwlClient prowl = new ProwlClient();
		ProwlEvent event = new DefaultProwlEvent(this.prowlApiKey, "Apple Order Status", this.itemName, message, 0);
		String response = null;
		try {
			response = prowl.pushEvent(event);
		} catch (ProwlException pe) {
			pe.printStackTrace();
		}
		
		return response;
	}
	
	public String[] getOrderDates(String pageContent) {
		String startSeq = "<div class=\"rightSubheader\">";
		String endSeq = "</td>";
		int startPos = pageContent.indexOf(startSeq) + startSeq.length();
		
		String orderDetail = pageContent.substring(startPos, pageContent.indexOf(endSeq, startPos));
		startSeq = "class=\"headerText\">";
		orderDetail = orderDetail.substring(orderDetail.indexOf(startSeq) + startSeq.length());
		
		String[] orderDetails = orderDetail.split("<br/>");
		
		return orderDetails;
	}

	public String getOrderStatus(String pageContent) {
		
		String startSeq = "<div class=\"headerTextwhite\">";
		String endSeq = "</div>";
		int startPos = pageContent.indexOf(startSeq) + startSeq.length();
		
		String orderStatus = pageContent.substring(startPos, pageContent.indexOf(endSeq, startPos));
		
		if (orderStatus != null) {
			orderStatus = orderStatus.trim();
		}
		
		return orderStatus;
	}
	
	private Properties loadOrderStatus() {
		Properties properties = new Properties();
		
		InputStream inStream = null;	
		try {
			inStream = new BufferedInputStream(new FileInputStream(new File(this.getPropertiesFilename())));	
			properties.load(inStream);
		} catch(IOException io) {
			System.out.println("Could not find order properties file");
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		return properties;
	}
	
	private void saveOrderStatus(String orderStatus, String shippingDate, String deliveryDate) {
		
		Properties properties = new Properties();
		
		properties.setProperty(ORDER_STATUS, orderStatus);
		properties.setProperty(SHIPPING_DATE, shippingDate);
		properties.setProperty(DELIVERY_DATE, deliveryDate);
		
		OutputStream outStream = null;	
		try {
			outStream = new BufferedOutputStream(new FileOutputStream(new File(this.getPropertiesFilename())));	
			properties.store(outStream, "Apple Order Details");
		} catch(IOException io) {
			System.out.println("Could not write order properties file");
		} finally {
			try {
				if (outStream != null) {
					outStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String getPropertiesFilename() {
		StringBuffer sb = new StringBuffer();
		sb.append(".");
		sb.append(this.orderNumber);
		sb.append(".properties");

		return sb.toString();
	}

	public String retrieveOrderPage() {
		StringBuffer sb = new StringBuffer();
		InputStream inStream = null;
		try {
			URL url = new URL(this.getOrderHttpUrl());
			URLConnection conn = url.openConnection();
			inStream = UrlConnection.openConnectionCheckRedirects(conn);
			
			int b;
			while ((b = inStream.read()) != -1) {
				sb.append((char) b);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return sb.toString();
	}
	
}
