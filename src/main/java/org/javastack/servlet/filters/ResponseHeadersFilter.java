package org.javastack.servlet.filters;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public final class ResponseHeadersFilter implements Filter {
	private static final String jvmName = getJvmname();
	private static final String hostName = getHostname();
	private static final int pid = getPID();
	private List<Header> earlyHeaders = null;
	private List<Header> lateHeaders = null;

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		final List<Header> earlyHeaders = new ArrayList<Header>();
		final List<Header> lateHeaders = new ArrayList<Header>();
		final Enumeration<String> e = filterConfig.getInitParameterNames();
		while (e.hasMoreElements()) {
			final String name = e.nextElement();
			final String[] nt = name.split(":");
			final String value = evalExpressionInit(filterConfig.getInitParameter(name));
			final TAG tag = ((nt.length < 2) ? TAG.SET : TAG.getTag(nt[1]));
			final TYPE type = ((nt.length < 3) ? TYPE.EARLY : TYPE.getType(nt[2]));
			if (type == TYPE.LATE) {
				lateHeaders.add(new Header(nt[0], tag, value));
			} else {
				earlyHeaders.add(new Header(nt[0], tag, value));
			}
		}
		this.earlyHeaders = Collections.unmodifiableList(earlyHeaders);
		this.lateHeaders = Collections.unmodifiableList(lateHeaders);
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, //
			final FilterChain chain) throws IOException, ServletException {
		if (response instanceof HttpServletResponse) {
			final HttpServletResponse res = ((HttpServletResponse) response);
			try {
				for (final Header e : earlyHeaders) {
					processHeader(res, e);
				}
				chain.doFilter(request, response);
			} finally {
				for (final Header e : lateHeaders) {
					processHeader(res, e);
				}
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	private final void processHeader(final HttpServletResponse res, final Header e) {
		switch (e.tag) {
			case SET: {
				res.setHeader(e.name, e.value);
				break;
			}
			case SETIFEMPTY: {
				final String ov = res.getHeader(e.name);
				if ((ov == null) || ov.isEmpty()) {
					res.setHeader(e.name, e.value);
				}
				break;
			}
			case ADD: {
				res.addHeader(e.name, e.value);
				break;
			}
			case ADDIFEXIST: {
				final String ov = res.getHeader(e.name);
				if ((ov != null) && !ov.isEmpty()) {
					res.addHeader(e.name, e.value);
				}
				break;
			}
		}
	}

	@Override
	public void destroy() {
	}

	private static String getJvmname() {
		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		try {
			return ManagementFactory.getRuntimeMXBean().getName();
		} catch (Exception e) {
		}
		return "";
	}

	private static String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final int index = jvmName.indexOf('@');
		if (index >= 1) {
			return jvmName.substring(index + 1);
		}
		return "unknown";
	}

	private static int getPID() {
		// Linux
		try {
			final File f = new File("/proc/self");
			if (f.exists()) {
				return Integer.parseInt(f.getCanonicalFile().getName());
			}
		} catch (Exception e) {
		}
		// something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
		final int index = jvmName.indexOf('@');
		if (index >= 1) {
			return Integer.parseInt(jvmName.substring(0, index));
		}
		return 0;
	}

	private static String getSystemValue(final String key) {
		switch (key) {
			case "HOSTNAME":
				return hostName;
			case "PID":
				return String.valueOf(pid);
			default:
				return null;
		}
	}

	private static String evalExpressionInit(final String input) {
		if (input == null) {
			return "";
		}
		final Pattern p = Pattern.compile("\\{\\{([A-Z]+):([^}]+)\\}\\}"); // {{TAG:name}}
		final Matcher m = p.matcher(input);
		final StringBuffer sb = new StringBuffer();
		while (m.find()) {
			final String tag = m.group(1); // ENV, PROP, SYS
			final String name = m.group(2).trim();
			final String value;
			switch (tag) {
				case "ENV":
					value = System.getenv(name);
					break;
				case "PROP":
					value = System.getProperty(name);
					break;
				case "SYS":
					value = getSystemValue(name);
					break;
				default:
					value = m.group(0); // untouched
			}
			m.appendReplacement(sb, Matcher.quoteReplacement((value != null) ? value : ""));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static enum TAG {
		SET, // *default
		SETIFEMPTY, //
		ADD, //
		ADDIFEXIST; //

		static TAG getTag(final String name) {
			try {
				return TAG.valueOf(name.trim().toUpperCase());
			} catch (Exception e) {
			}
			return TAG.SET;
		}
	}

	private static enum TYPE {
		EARLY, // *default
		LATE; //

		static TYPE getType(final String name) {
			try {
				return TYPE.valueOf(name.trim().toUpperCase());
			} catch (Exception e) {
			}
			return TYPE.EARLY;
		}
	}

	private static class Header {
		final String name;
		final TAG tag;
		final String value;

		public Header(final String name, final TAG tag, final String value) {
			this.name = name;
			this.tag = tag;
			this.value = value;
		}
	}
}