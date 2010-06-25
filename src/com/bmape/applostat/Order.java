package com.bmape.applostat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.bmape.applostat.util.UrlConnection;

/**
 * @author martinoswald
 */
public class Order {
	
	private String orderNumber;
	private String emailAddress;
	private String pageContent;
	
	public Order(String orderNumber, String emailAddress) {
		this.orderNumber = orderNumber;
		this.emailAddress = emailAddress;
		this.pageContent = this.retrieveOrderPage();
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
	
	private String retrieveOrderPage() {
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
	
	public String[] getOrderDates() {
		String startSeq = "<div class=\"rightSubheader\">";
		String endSeq = "</td>";
		int startPos = this.pageContent.indexOf(startSeq) + startSeq.length();
		
		String orderDetail = this.pageContent.substring(startPos, this.pageContent.indexOf(endSeq, startPos));
		startSeq = "class=\"headerText\">";
		orderDetail = orderDetail.substring(orderDetail.indexOf(startSeq) + startSeq.length());
		
		String[] orderDetails = orderDetail.split("<br/>");
		
		return orderDetails;
	}

	public String getOrderStatus() {
		
		String startSeq = "<div class=\"headerTextwhite\">";
		String endSeq = "</div>";
		int startPos = this.pageContent.indexOf(startSeq) + startSeq.length();
		
		String orderStatus = this.pageContent.substring(startPos, this.pageContent.indexOf(endSeq, startPos));
		
		if (orderStatus != null) {
			orderStatus = orderStatus.trim();
		}
		
		return orderStatus;
	}
	
}
