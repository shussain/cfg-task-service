package ca.gc.ip346.classification.resource;

import static javax.ws.rs.HttpMethod.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
// import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import com.google.gson.GsonBuilder;

import ca.gc.ip346.util.ClassificationProperties;
import ca.gc.ip346.util.RequestURL;

@Path("/rulesets")
// @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class RulesResource {
	private static final Logger logger = LogManager.getLogger(RulesResource.class);
	private Map<String, String> map = null;

	@Context
	private HttpServletRequest request;

	public RulesResource() {
		map = new HashMap<String, String>();
	}

	/**
	 * Sprint 5 - Build REST service to return rulesets
	 */
	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response getRules() {
		// String target = request.getRequestURL().toString().replaceAll("(\\w+:\\/\\/[^/]+(:\\d+)?/[^/]+).*", "$1").replaceAll("-task-", "-classification-");
		String target = RequestURL.getAddr() + ClassificationProperties.getEndPoint();
		map.put("message", "REST service to return rulesets");
		logger.debug("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");
		logger.debug("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(target) + "[00;00m");

		Response response = ClientBuilder
			.newClient()
			.target(target)
			.path("/rulesets")
			.request()
			.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE)
			.accept(MediaType.APPLICATION_JSON)
			.get();

		// return FoodsResource.getResponse(GET, Response.Status.OK, response.readEntity(Object.class));
		return FoodsResource.getResponse(GET, Response.Status.OK, response.readEntity(new GenericType<HashMap<String, Object>>() {}));
	}

	/**
	 * Sprint 5 - Build REST service to return a particular ruleset
	 */
	@GET
	@Path("/{id}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response selectRules(@PathParam("id") String id) {
		// String target = request.getRequestURL().toString().replaceAll("(\\w+:\\/\\/[^/]+(:\\d+)?/[^/]+).*", "$1").replaceAll("-task-", "-classification-");
		String target = RequestURL.getAddr() + ClassificationProperties.getEndPoint();
		map.put("message", "REST service to return a particular ruleset");
		logger.debug("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");

		Response response = ClientBuilder
			.newClient()
			.target(target)
			.path("/rulesets/" + id)
			.request()
			.get();

		return FoodsResource.getResponse(GET, Response.Status.OK, response.readEntity(Object.class));
	}

	/**
	 * Sprint 5 - Build REST service to create new ruleset
	 */
	@POST
	public Response createRules() {
		map.put("message", "REST service to create new ruleset");
		logger.debug("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");
		return FoodsResource.getResponse(POST, Response.Status.OK, map);
	}

	/**
	 * Sprint 5 - Build REST service to update an existing ruleset
	 */
	@PUT
	@Path("id")
	public Response updateRules(@PathParam("id") String id) {
		map.put("message", "REST service to update an existing ruleset");
		logger.debug("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");
		return FoodsResource.getResponse(PUT, Response.Status.OK, map);
	}

	/**
	 * Sprint 5 - Build REST service to delete an existing ruleset
	 */
	@DELETE
	@Path("/{id}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response deleteRules(@PathParam("id") String id) {
		// String target = request.getRequestURL().toString().replaceAll("(\\w+:\\/\\/[^/]+(:\\d+)?/[^/]+).*", "$1").replaceAll("-task-", "-classification-");
		String target = RequestURL.getAddr() + ClassificationProperties.getEndPoint();
		map.put("message", "REST service to delete an existing ruleset");
		logger.debug("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");

		Response response = ClientBuilder
			.newClient()
			.target(target)
			.path("/rulesets/" + id)
			.request()
			.delete();

		return FoodsResource.getResponse(DELETE, Response.Status.OK, response.readEntity(Object.class));
	}
}
