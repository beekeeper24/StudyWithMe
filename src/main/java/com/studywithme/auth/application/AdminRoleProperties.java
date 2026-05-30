package com.studywithme.auth.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public class AdminRoleProperties {

	private List<String> emails = List.of();

	public List<String> getEmails() {
		return emails;
	}

	public void setEmails(List<String> emails) {
		this.emails = emails == null ? List.of() : emails;
	}
}
