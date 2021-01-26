package com.springrest.springrest.interceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

@Component
public class APICallInterceptor implements HandlerInterceptor{
	
	// Max capacity of the bucket
	private static final int BUCKET_CAPACITY = 1;

	// Refill duration of the bucket in minutes
	private static final int REFILL_DURATION = 1;

	// Number of tokens to be consumed at a time from bucket
	private static final int TOKENS_TO_CONSUME = 1;

	// Map of buckets per IP addresses
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	// Common bucket if no IP address is found
	private final Bucket bucket = assignBucket();
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		// Gets IP address from request header
		String xfHeader = request.getHeader("X-FORWARDED-FOR");
		String ipAddress = null;
		if (xfHeader == null) {
		ipAddress = request.getRemoteAddr();
		} else {
		ipAddress = xfHeader.split(",")[0];
		}

		// Gets the bucket of current IP address or assign a new bucket if not present
		Bucket requestBucket;
		if (StringUtils.hasText(ipAddress)) {
			requestBucket = this.buckets.computeIfAbsent(ipAddress, ip -> assignBucket());
		} else {
			requestBucket = this.bucket;
		}

		// Consumes a token from current client's bucket
		ConsumptionProbe probe = requestBucket.tryConsumeAndReturnRemaining(TOKENS_TO_CONSUME);

		/*
		* Returns true if token is consumed and add remaining tokens in response header
		* Returns false if no token is consumed, set response status and add time to refill bucket in response header
		*/
		if (probe.isConsumed()) {
		response.addHeader("X-Rate-Limit-Remaining",
		Long.toString(probe.getRemainingTokens()));
		return true;
		}
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
		response.addHeader("X-Rate-Limit-Retry-After-Milliseconds",
		Long.toString(TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill())));

		return false;
	}
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
	}
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
	}
	
	// Returns a new bucket for new IP address
	private static Bucket assignBucket() {
	return Bucket4j.builder()
	.addLimit(Bandwidth.classic(BUCKET_CAPACITY, Refill.intervally(BUCKET_CAPACITY, Duration.ofMinutes(REFILL_DURATION))))
	.build();
	}
}
