/**
 * 
 */
package com.bmape.applostat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import net.sourceforge.prowl.api.DefaultProwlEvent;
import net.sourceforge.prowl.api.ProwlClient;
import net.sourceforge.prowl.api.ProwlEvent;
import net.sourceforge.prowl.exception.ProwlException;

import com.bmape.applostat.util.Constants;
import com.bmape.applostat.util.UrlConnection;
import com.bmape.applostat.util.Utils;

/**
 * 
 * @author martinoswald
 */
public class Applostat {
	
	private String orderNumber;
	private String emailAddress;
	private String itemName;
	private String prowlApiKey;
	
	private String orderStatus;
	private String shippingDate;
	private String deliveryDate;

	public Applostat(File configFile) {
		super();
		
		Properties properties = Utils.readPropertiesFile(configFile);
		
		this.orderNumber = properties.getProperty(Constants.PROP_ORDER_NUMBER);
		this.emailAddress = properties.getProperty(Constants.PROP_EMAIL_ADDRESS);
		this.itemName = properties.getProperty(Constants.PROP_ITEM_NAME, "");
		this.prowlApiKey = properties.getProperty(Constants.PROP_PROWL_API_KEY);
		
	}

	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.out.println("OrderStatus <config file>");
			return;
		}
		
		File configFile = new File(args[0]);
		
		Applostat applostat = new Applostat(configFile);
		
		applostat.receiveOrderDetails();
		
		applostat.sendNotification();

//		System.out.println("Start retrieving order status ...");
//		System.out.println("Locating order status ...");
//		System.out.println("OrderStatus: " + orderStatus);
//		System.out.println("Locating order dates ...");
//		System.out.println("Shipping-Date: " + shippingDate);
//		System.out.println("Delivery-Date: " + deliveryDate);
//		System.out.println("Check if order details changed ...");
//		System.out.println("Finished");
		
	}
	
	public void receiveOrderDetails() {
		
		Order order = new Order(this.orderNumber, this.emailAddress);
		
		this.orderStatus = order.getOrderStatus();
		
		String[] orderDates = order.getOrderDates();
		this.shippingDate = orderDates[0].trim();
		this.deliveryDate = orderDates[1].trim();
	}
	
	public void sendNotification() {
		
		String message = "Status: " + this.orderStatus + "\n" + this.shippingDate + "\n" + this.deliveryDate;
		
		boolean sendNotifaction = false;
		Properties properties = this.loadOrderStatus();
		if (properties.size() != 3) {
			this.saveOrderStatus(this.orderStatus, this.shippingDate, this.deliveryDate);
			sendNotifaction = true;
			
		} else {
			
			String propOrderStatus = properties.getProperty(Constants.PROP_ORDER_STATUS);
			String propShippingDate = properties.getProperty(Constants.PROP_SHIPPING_DATE);
			String propDeliveryDate = properties.getProperty(Constants.PROP_DELIVERY_DATE);
			
			if (propOrderStatus == null || !propOrderStatus.equals(this.orderStatus)) {
				System.out.println("Order status changed");
				message = "Order status changed\n" + message;
				sendNotifaction = true;
				
			} 
			if (propShippingDate == null || !propShippingDate.equals(this.shippingDate)) {
				System.out.println("Shipping date changed");
				message = "Shipping date changed\n" + message;
				sendNotifaction = true;
				
			} 
			if (propDeliveryDate == null || !propDeliveryDate.equals(this.deliveryDate)) {
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
	
	private Properties loadOrderStatus() {
		File file = new File(this.getPropertiesFilename());
		
		return Utils.readPropertiesFile(file);
	}
	
	private void saveOrderStatus(String orderStatus, String shippingDate, String deliveryDate) {
		
		Properties properties = new Properties();
		properties.setProperty(Constants.PROP_ORDER_STATUS, orderStatus);
		properties.setProperty(Constants.PROP_SHIPPING_DATE, shippingDate);
		properties.setProperty(Constants.PROP_DELIVERY_DATE, deliveryDate);
		
		String comments = "Apple Order Details";
		File file = new File(this.getPropertiesFilename());
		Utils.writePropertiesFile(properties, file, comments);
	}

	private String getPropertiesFilename() {
		StringBuffer sb = new StringBuffer();
		sb.append(".");
		sb.append(this.orderNumber);
		sb.append(".properties");

		return sb.toString();
	}

	
}
