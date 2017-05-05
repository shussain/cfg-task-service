package ca.gc.ip346.classification.resource;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.currentDate;
import static com.mongodb.client.model.Updates.set;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import com.google.common.net.HttpHeaders;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

// import ca.gc.ip346.classification.model.Added;
import ca.gc.ip346.classification.model.CanadaFoodGuideDataset;
// import ca.gc.ip346.classification.model.NewAndImprovedFoodItem;
import ca.gc.ip346.classification.model.CfgFilter;
import ca.gc.ip346.classification.model.CfgTier;
import ca.gc.ip346.classification.model.ContainsAdded;
import ca.gc.ip346.classification.model.Dataset;
import ca.gc.ip346.classification.model.FoodItem;
import ca.gc.ip346.classification.model.Missing;
import ca.gc.ip346.classification.model.RecipeRolled;
import ca.gc.ip346.util.DBConnection;
import ca.gc.ip346.util.MongoClientFactory;
import ca.gc.ip346.util.RequestURI;

@Path("/datasets")
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class FoodsResource {
	private static final Logger logger = LogManager.getLogger(FoodsResource.class);

	private Connection conn                      = null;
	private DatabaseMetaData meta                = null;
	private MongoClient mongoClient              = null;
	private MongoCollection<Document> collection = null;

	public FoodsResource() {
		mongoClient = MongoClientFactory.getMongoClient();
		collection  = mongoClient.getDatabase(MongoClientFactory.getDatabase()).getCollection(MongoClientFactory.getCollection());

		try {
			conn = DBConnection.getConnections();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@GET
	@Path("/search")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public /* List<CanadaFoodGuideDataset> */ Response getFoodList(@BeanParam CfgFilter search) {
		String sql = ContentHandler.read("canada_food_guide_dataset.sql", getClass());
		search.setSql(sql);
		return doSearchCriteria(search);
	}

	@POST
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public /* Map<String, Object> */ Response saveDataset(Dataset dataset) {
		ResponseBuilder response = null;

		Map<String, Object> map = new HashMap<String, Object>();

		if (dataset.getData() != null && dataset.getName() != null && dataset.getComments() != null) {
			Document doc = new Document()
				.append("data",     dataset.getData())
				.append("name",     dataset.getName())
				.append("env",      dataset.getEnv())
				.append("owner",    dataset.getOwner())
				.append("status",   dataset.getStatus())
				.append("comments", dataset.getComments());
			collection.insertOne(doc);
			ObjectId id = (ObjectId)doc.get("_id");
			collection.updateOne(
					eq("_id", id),
					combine(
						set("name", dataset.getName()),
						set("comments", dataset.getComments()),
						currentDate("modifiedDate"))
					);

			logger.error("[01;34mLast inserted Dataset id: " + id + "[00;00m");

			logger.error("[01;34mCurrent number of Datasets: " + collection.count() + "[00;00m");

			map.put("id", id.toString());

			logger.error("[01;34m" + Response.Status.CREATED.getStatusCode() + " " + Response.Status.CREATED.toString() + "[00;00m");

			response = Response.status(Response.Status.CREATED);
		} else {
			List<String> list = new ArrayList<String>();

			if (dataset.getData()     == null) list.add("data");
			if (dataset.getName()     == null) list.add("name");
			if (dataset.getEnv()      == null) list.add("env");
			if (dataset.getComments() == null) list.add("comments");

			map.put("code", Response.Status.BAD_REQUEST.getStatusCode());
			map.put("description", Response.Status.BAD_REQUEST.toString() + " - Unable to insert Dataset!");
			map.put("fields", StringUtils.join(list, ", "));

			logger.error("[01;34m" + Response.Status.BAD_REQUEST.toString() + " - Unable to insert Dataset!" + "[00;00m");
			logger.error("[01;34m" + Response.Status.BAD_REQUEST.getStatusCode() + " " + Response.Status.BAD_REQUEST.toString() + "[00;00m");

			response = Response.status(Response.Status.BAD_REQUEST);
		}

		mongoClient.close();

		return response
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
			.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1209600")
			.entity(map).build();
	}

	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public /* List<Map<String, String>> */ Response getDatasets(@QueryParam("env") String env) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		MongoCursor<Document> cursorDocMap = collection.find(eq("env", env)).iterator();
		while (cursorDocMap.hasNext()) {
			Map<String, String> map = new HashMap<String, String>();
			Document doc = cursorDocMap.next();
			map.put("id", doc.get("_id").toString());

			if (doc.get("name"        ) != null) map.put("name",         doc.get("name"        ).toString());
			if (doc.get("env"         ) != null) map.put("env",          doc.get("env"         ).toString());
			if (doc.get("owner"       ) != null) map.put("owner",        doc.get("owner"       ).toString());
			if (doc.get("status"      ) != null) map.put("status",       doc.get("status"      ).toString());
			if (doc.get("comments"    ) != null) map.put("comments",     doc.get("comments"    ).toString());
			if (doc.get("modifiedDate") != null) map.put("modifiedDate", doc.get("modifiedDate").toString());

			list.add(map);
			logger.error("[01;34mDataset ID: " + doc.get("_id") + "[00;00m");
		}

		mongoClient.close();

		return Response.status(Response.Status.OK)
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
			.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1209600")
			.entity(list).build();
	}

	@GET
	@Path("/{id}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public /* List<Map<String, Object>> */ Response getDataset(@PathParam("id") String id) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		MongoCursor<Document> cursorDocMap = collection.find(new Document("_id", new ObjectId(id))).iterator();
		while (cursorDocMap.hasNext()) {
			Map<String, Object> map = new HashMap<String, Object>();
			Document doc = cursorDocMap.next();
			logger.error("[01;34mDataset ID: " + doc.get("_id") + "[00;00m");

			if (doc != null) {
				map.put("id",           id);
				map.put("data",         doc.get("data"));
				map.put("name",         doc.get("name"));
				map.put("env",          doc.get("env"));
				map.put("owner",        doc.get("owner"));
				map.put("status",       doc.get("status"));
				map.put("comments",     doc.get("comments"));
				map.put("modifiedDate", doc.get("modifiedDate"));
				list.add(map);
			}
		}

		mongoClient.close();

		return Response.status(Response.Status.OK)
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
			.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1209600")
			.entity(list.get(0)).build();
	}

	@DELETE
	@Path("/{id}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void deleteDataset(@PathParam("id") String id) {
		MongoCursor<Document> cursorDocMap = collection.find(new Document("_id", new ObjectId(id))).iterator();
		while (cursorDocMap.hasNext()) {
			Document doc = cursorDocMap.next();
			logger.error("[01;34mDataset ID: " + doc.get("_id") + "[00;00m");

			collection.deleteOne(doc);
		}

		mongoClient.close();
	}

	@DELETE
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response deleteAllDatasets() {
		collection.deleteMany(new Document());

		mongoClient.close();

		Map<String, String> msg = new HashMap<String, String>();
		msg.put("message", "Successfully deleted all datasets");

		return Response.status(Response.Status.OK)
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
			.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1209600")
			.entity(msg).build();
	}

	@PUT
	@Path("/{id}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response updateDataset(@PathParam("id") String id, Dataset dataset) {
		Map<Integer, Map<String, Object>> original_values_map = new HashMap<Integer, Map<String, Object>>();
		Map<Integer, Map<String, Object>> toupdate_values_map = new HashMap<Integer, Map<String, Object>>();
		List<Object> list = null;
		List<Bson> firstLevelSets = new ArrayList<Bson>();
		int changes = 0;

		// retrive the corresponding dataset with the given id
		MongoCursor<Document> cursorDocMap = collection.find(new Document("_id", new ObjectId(id))).iterator();
		while (cursorDocMap.hasNext()) {
			Document doc = cursorDocMap.next();

			list = castList(doc.get("data"), Object.class);
			for (Object obj : list) {
				Map<?, ?> mObj = (Map<?, ?>)obj;
				Map<String, Object> tmp = new HashMap<String, Object>();
				Iterator<?> it = mObj.keySet().iterator();
				while (it.hasNext()) {
					String key = (String)it.next();
					tmp.put(key, mObj.get(key));
				}
				original_values_map.put((Integer)tmp.get("code"), tmp);
			}

			if (!dataset.getName     ().equals(doc.get("name")    )) {
				firstLevelSets.add(set("name", dataset.getName()));
				++changes;
			}
			if (!dataset.getEnv      ().equals(doc.get("env")     )) {
				firstLevelSets.add(set("env", dataset.getEnv()));
				++changes;
			}
			if (!dataset.getOwner    ().equals(doc.get("owner")   )) {
				firstLevelSets.add(set("owner", dataset.getOwner()));
				++changes;
			}
			if (!dataset.getStatus   ().equals(doc.get("status")  )) {
				firstLevelSets.add(set("status", dataset.getStatus()));
				++changes;
			}
			if (!dataset.getComments ().equals(doc.get("comments"))) {
				firstLevelSets.add(set("comments", dataset.getComments()));
				++changes;
			}
		}

		List<Map<String, Object>> updates = dataset.getData();
		for (Map<String, Object> map : updates) {
			toupdate_values_map.put((Integer)map.get("code"), map);

			logger.error("[01;34mDataset: " + toupdate_values_map.get(map.get("code")) + "[00;00m");

			logger.error("[01;31mname: " + toupdate_values_map.get(map.get("code")).get("name") + "[00;00m");
		}

		for (Map<String, Object> map : updates) {
			List<Bson> sets = new ArrayList<Bson>();

			logger.error("[01;31msize: " + sets.size() + "[00;00m");

			if (toupdate_values_map .get (map .get ("code")) .get ("name")                               != null && !toupdate_values_map .get (map .get ("code")) .get ("name")                               .equals (original_values_map .get (map .get ("code")) .get ("name"))) {
				sets.add(set("data.$.name", map.get("name")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("name") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("cnfGroupCode")                       != null && !toupdate_values_map .get (map .get ("code")) .get ("cnfGroupCode")                       .equals (original_values_map .get (map .get ("code")) .get ("cnfGroupCode"))) {
				sets.add(set("data.$.cnfGroupCode", map.get("cnfGroupCode")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("cnfGroupCode") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("cfgCode")                            != null && !toupdate_values_map .get (map .get ("code")) .get ("cfgCode")                            .equals (original_values_map .get (map .get ("code")) .get ("cfgCode"))) {
				sets.add(set("data.$.cfgCode", map.get("cfgCode")));
				sets.add(currentDate("data.$.cfgCodeUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("cfgCode") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("cfgCodeUpdateDate")                  != null && !toupdate_values_map .get (map .get ("code")) .get ("cfgCodeUpdateDate")                  .equals (original_values_map .get (map .get ("code")) .get ("cfgCodeUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("energyKcal")                         != null && !toupdate_values_map .get (map .get ("code")) .get ("energyKcal")                         .equals (original_values_map .get (map .get ("code")) .get ("energyKcal"))) {
				sets.add(set("data.$.energyKcal", map.get("energyKcal")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("energyKcal") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("sodiumAmountPer100g")                != null && !toupdate_values_map .get (map .get ("code")) .get ("sodiumAmountPer100g")                .equals (original_values_map .get (map .get ("code")) .get ("sodiumAmountPer100g"))) {
				sets.add(set("data.$.sodiumAmountPer100g", map.get("sodiumAmountPer100g")));
				sets.add(currentDate("data.$.sodiumImputationDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("sodiumAmountPer100g") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("sodiumImputationReference")          != null && !toupdate_values_map .get (map .get ("code")) .get ("sodiumImputationReference")          .equals (original_values_map .get (map .get ("code")) .get ("sodiumImputationReference"))) {
				sets.add(set("data.$.sodiumImputationReference", map.get("sodiumImputationReference")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("sodiumImputationReference") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("sodiumImputationDate")               != null && !toupdate_values_map .get (map .get ("code")) .get ("sodiumImputationDate")               .equals (original_values_map .get (map .get ("code")) .get ("sodiumImputationDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("sugarAmountPer100g")                 != null && !toupdate_values_map .get (map .get ("code")) .get ("sugarAmountPer100g")                 .equals (original_values_map .get (map .get ("code")) .get ("sugarAmountPer100g"))) {
				sets.add(set("data.$.sugarAmountPer100g", map.get("sugarAmountPer100g")));
				sets.add(currentDate("data.$.sugarImputationDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("sugarAmountPer100g") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("sugarImputationReference")           != null && !toupdate_values_map .get (map .get ("code")) .get ("sugarImputationReference")           .equals (original_values_map .get (map .get ("code")) .get ("sugarImputationReference"))) {
				sets.add(set("data.$.sugarImputationReference", map.get("sugarImputationReference")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("sugarImputationReference") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("sugarImputationDate")                != null && !toupdate_values_map .get (map .get ("code")) .get ("sugarImputationDate")                .equals (original_values_map .get (map .get ("code")) .get ("sugarImputationDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("transfatAmountPer100g")              != null && !toupdate_values_map .get (map .get ("code")) .get ("transfatAmountPer100g")              .equals (original_values_map .get (map .get ("code")) .get ("transfatAmountPer100g"))) {
				sets.add(set("data.$.transfatAmountPer100g", map.get("transfatAmountPer100g")));
				sets.add(currentDate("data.$.transfatImputationDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("transfatAmountPer100g") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("transfatImputationReference")        != null && !toupdate_values_map .get (map .get ("code")) .get ("transfatImputationReference")        .equals (original_values_map .get (map .get ("code")) .get ("transfatImputationReference"))) {
				sets.add(set("data.$.transfatImputationReference", map.get("transfatImputationReference")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("transfatImputationReference") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("transfatImputationDate")             != null && !toupdate_values_map .get (map .get ("code")) .get ("transfatImputationDate")             .equals (original_values_map .get (map .get ("code")) .get ("transfatImputationDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("satfatAmountPer100g")                != null && !toupdate_values_map .get (map .get ("code")) .get ("satfatAmountPer100g")                .equals (original_values_map .get (map .get ("code")) .get ("satfatAmountPer100g"))) {
				sets.add(set("data.$.satfatAmountPer100g", map.get("satfatAmountPer100g")));
				sets.add(currentDate("data.$.satfatImputationDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("satfatAmountPer100g") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("satfatImputationReference")          != null && !toupdate_values_map .get (map .get ("code")) .get ("satfatImputationReference")          .equals (original_values_map .get (map .get ("code")) .get ("satfatImputationReference"))) {
				sets.add(set("data.$.satfatImputationReference", map.get("satfatImputationReference")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("satfatImputationReference") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("satfatImputationDate")               != null && !toupdate_values_map .get (map .get ("code")) .get ("satfatImputationDate")               .equals (original_values_map .get (map .get ("code")) .get ("satfatImputationDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("totalfatAmountPer100g")              != null && !toupdate_values_map .get (map .get ("code")) .get ("totalfatAmountPer100g")              .equals (original_values_map .get (map .get ("code")) .get ("totalfatAmountPer100g"))) {
				sets.add(set("data.$.totalfatAmountPer100g", map.get("totalfatAmountPer100g")));
				sets.add(currentDate("data.$.totalfatImputationDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("totalfatAmountPer100g") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("totalfatImputationReference")        != null && !toupdate_values_map .get (map .get ("code")) .get ("totalfatImputationReference")        .equals (original_values_map .get (map .get ("code")) .get ("totalfatImputationReference"))) {
				sets.add(set("data.$.totalfatImputationReference", map.get("totalfatImputationReference")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("totalfatImputationReference") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("totalfatImputationDate")             != null && !toupdate_values_map .get (map .get ("code")) .get ("totalfatImputationDate")             .equals (original_values_map .get (map .get ("code")) .get ("totalfatImputationDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedSodium")                != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedSodium")                .equals (original_values_map .get (map .get ("code")) .get ("containsAddedSodium"))) {
				sets.add(set("data.$.containsAddedSodium", map.get("containsAddedSodium")));
				sets.add(currentDate("data.$.containsAddedSodiumUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsAddedSodium") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedSodiumUpdateDate")      != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedSodiumUpdateDate")      .equals (original_values_map .get (map .get ("code")) .get ("containsAddedSodiumUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedSugar")                 != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedSugar")                 .equals (original_values_map .get (map .get ("code")) .get ("containsAddedSugar"))) {
				sets.add(set("data.$.containsAddedSugar", map.get("containsAddedSugar")));
				sets.add(currentDate("data.$.containsAddedSugarUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsAddedSugar") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedSugarUpdateDate")       != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedSugarUpdateDate")       .equals (original_values_map .get (map .get ("code")) .get ("containsAddedSugarUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsFreeSugars")                 != null && !toupdate_values_map .get (map .get ("code")) .get ("containsFreeSugars")                 .equals (original_values_map .get (map .get ("code")) .get ("containsFreeSugars"))) {
				sets.add(set("data.$.containsFreeSugars", map.get("containsFreeSugars")));
				sets.add(currentDate("data.$.containsFreeSugarsUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsFreeSugars") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsFreeSugarsUpdateDate")       != null && !toupdate_values_map .get (map .get ("code")) .get ("containsFreeSugarsUpdateDate")       .equals (original_values_map .get (map .get ("code")) .get ("containsFreeSugarsUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedFat")                   != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedFat")                   .equals (original_values_map .get (map .get ("code")) .get ("containsAddedFat"))) {
				sets.add(set("data.$.containsAddedFat", map.get("containsAddedFat")));
				sets.add(currentDate("data.$.containsAddedFatUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsAddedFat") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedFatUpdateDate")         != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedFatUpdateDate")         .equals (original_values_map .get (map .get ("code")) .get ("containsAddedFatUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedTransfat")              != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedTransfat")              .equals (original_values_map .get (map .get ("code")) .get ("containsAddedTransfat"))) {
				sets.add(set("data.$.containsAddedTransfat", map.get("containsAddedTransfat")));
				sets.add(currentDate("data.$.containsAddedTransfatUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsAddedTransfat") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsAddedTransfatUpdateDate")    != null && !toupdate_values_map .get (map .get ("code")) .get ("containsAddedTransfatUpdateDate")    .equals (original_values_map .get (map .get ("code")) .get ("containsAddedTransfatUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsCaffeine")                   != null && !toupdate_values_map .get (map .get ("code")) .get ("containsCaffeine")                   .equals (original_values_map .get (map .get ("code")) .get ("containsCaffeine"))) {
				sets.add(set("data.$.containsCaffeine", map.get("containsCaffeine")));
				sets.add(currentDate("data.$.containsCaffeineUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsCaffeine") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsCaffeineUpdateDate")         != null && !toupdate_values_map .get (map .get ("code")) .get ("containsCaffeineUpdateDate")         .equals (original_values_map .get (map .get ("code")) .get ("containsCaffeineUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsSugarSubstitutes")           != null && !toupdate_values_map .get (map .get ("code")) .get ("containsSugarSubstitutes")           .equals (original_values_map .get (map .get ("code")) .get ("containsSugarSubstitutes"))) {
				sets.add(set("data.$.containsSugarSubstitutes", map.get("containsSugarSubstitutes")));
				sets.add(currentDate("data.$.containsSugarSubstitutesUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("containsSugarSubstitutes") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("containsSugarSubstitutesUpdateDate") != null && !toupdate_values_map .get (map .get ("code")) .get ("containsSugarSubstitutesUpdateDate") .equals (original_values_map .get (map .get ("code")) .get ("containsSugarSubstitutesUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("referenceAmountG")                   != null && !toupdate_values_map .get (map .get ("code")) .get ("referenceAmountG")                   .equals (original_values_map .get (map .get ("code")) .get ("referenceAmountG"))) {
				sets.add(set("data.$.referenceAmountG", map.get("referenceAmountG")));
				sets.add(currentDate("data.$.referenceAmountUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("referenceAmountG") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("referenceAmountMeasure")             != null && !toupdate_values_map .get (map .get ("code")) .get ("referenceAmountMeasure")             .equals (original_values_map .get (map .get ("code")) .get ("referenceAmountMeasure"))) {
				sets.add(set("data.$.referenceAmountMeasure", map.get("referenceAmountMeasure")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("referenceAmountMeasure") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("referenceAmountUpdateDate")          != null && !toupdate_values_map .get (map .get ("code")) .get ("referenceAmountUpdateDate")          .equals (original_values_map .get (map .get ("code")) .get ("referenceAmountUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("foodGuideServingG")                  != null && !toupdate_values_map .get (map .get ("code")) .get ("foodGuideServingG")                  .equals (original_values_map .get (map .get ("code")) .get ("foodGuideServingG"))) {
				sets.add(set("data.$.foodGuideServingG", map.get("foodGuideServingG")));
				sets.add(currentDate("data.$.foodGuideUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("foodGuideServingG") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("foodGuideServingMeasure")            != null && !toupdate_values_map .get (map .get ("code")) .get ("foodGuideServingMeasure")            .equals (original_values_map .get (map .get ("code")) .get ("foodGuideServingMeasure"))) {
				sets.add(set("data.$.foodGuideServingMeasure", map.get("foodGuideServingMeasure")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("foodGuideServingMeasure") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("foodGuideUpdateDate")                != null && !toupdate_values_map .get (map .get ("code")) .get ("foodGuideUpdateDate")                .equals (original_values_map .get (map .get ("code")) .get ("foodGuideUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("tier4ServingG")                      != null && !toupdate_values_map .get (map .get ("code")) .get ("tier4ServingG")                      .equals (original_values_map .get (map .get ("code")) .get ("tier4ServingG"))) {
				sets.add(set("data.$.tier4ServingG", map.get("tier4ServingG")));
				sets.add(currentDate("data.$.tier4ServingUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("tier4ServingG") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("tier4ServingMeasure")                != null && !toupdate_values_map .get (map .get ("code")) .get ("tier4ServingMeasure")                .equals (original_values_map .get (map .get ("code")) .get ("tier4ServingMeasure"))) {
				sets.add(set("data.$.tier4ServingMeasure", map.get("tier4ServingMeasure")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("tier4ServingMeasure") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("tier4ServingUpdateDate")             != null && !toupdate_values_map .get (map .get ("code")) .get ("tier4ServingUpdateDate")             .equals (original_values_map .get (map .get ("code")) .get ("tier4ServingUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("rolledUp")                           != null && !toupdate_values_map .get (map .get ("code")) .get ("rolledUp")                           .equals (original_values_map .get (map .get ("code")) .get ("rolledUp"))) {
				sets.add(set("data.$.rolledUp", map.get("rolledUp")));
				sets.add(currentDate("data.$.rolledUpUpdateDate"));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("rolledUp") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("rolledUpUpdateDate")                 != null && !toupdate_values_map .get (map .get ("code")) .get ("rolledUpUpdateDate")                 .equals (original_values_map .get (map .get ("code")) .get ("rolledUpUpdateDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("applySmallRaAdjustment")             != null && !toupdate_values_map .get (map .get ("code")) .get ("applySmallRaAdjustment")             .equals (original_values_map .get (map .get ("code")) .get ("applySmallRaAdjustment"))) {
				sets.add(set("data.$.applySmallRaAdjustment", map.get("applySmallRaAdjustment")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("applySmallRaAdjustment") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("replacementCode")                    != null && !toupdate_values_map .get (map .get ("code")) .get ("replacementCode")                    .equals (original_values_map .get (map .get ("code")) .get ("replacementCode"))) {
				sets.add(set("data.$.replacementCode", map.get("replacementCode")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("replacementCode") + "[00;00m");
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("commitDate")                         != null && !toupdate_values_map .get (map .get ("code")) .get ("commitDate")                         .equals (original_values_map .get (map .get ("code")) .get ("commitDate"))) {
			}
			if (toupdate_values_map .get (map .get ("code")) .get ("comments")                           != null && !toupdate_values_map .get (map .get ("code")) .get ("comments")                           .equals (original_values_map .get (map .get ("code")) .get ("comments"))) {
				sets.add(set("data.$.comments", map.get("comments")));
				++changes;
				logger.error("[01;31mvalue changed: " + map.get("comments") + "[00;00m");
			}

			logger.error("[01;31msize: " + sets.size() + "[00;00m");

			logger.error("[01;31mcode: " + map.get("code") + "[00;00m");

			if (sets.size() > 0) {
				collection.updateOne(and(eq("_id", new ObjectId(id)), eq("data.code", map.get("code"))), combine(sets));
			}
		}

		if (changes != 0) {
			firstLevelSets.add(currentDate("modifiedDate"));
			collection.updateOne(eq("_id", new ObjectId(id)), combine(firstLevelSets));
		}

		// cursorDocMap = collection.find(new Document("_id", new ObjectId(id))).iterator();
		// while (cursorDocMap.hasNext()) {
			// Document doc = cursorDocMap.next();

			// list = castList(doc.get("data"), Object.class);
		// }

		mongoClient.close();

		Map<String, String> msg = new HashMap<String, String>();
		msg.put("message", "Successfully updated dataset");

		return Response.status(Response.Status.OK)
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
			.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1209600")
			.entity(msg).build();
	}

	@POST
	@Path("/{id}/classify")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response classifyDataset(@PathParam("id") String id) {
		Map<String, Object> map = null;
		MongoCursor<Document> cursorDocMap = collection.find(new Document("_id", new ObjectId(id))).iterator();
		while (cursorDocMap.hasNext()) {
			map = new HashMap<String, Object>();
			Document doc = cursorDocMap.next();
			logger.error("[01;34mDataset ID: " + doc.get("_id") + "[00;00m");

			if (doc != null) {
				map.put("data",         doc.get("data"));
				map.put("name",         doc.get("name"));
				map.put("env",          doc.get("env"));
				map.put("owner",        doc.get("owner"));
				map.put("status",       doc.get("status"));
				map.put("comments",     doc.get("comments"));
				map.put("modifiedDate", doc.get("modifiedDate"));
			}
		}

		mongoClient.close();

		Response response = ClientBuilder
			.newClient()
			.target(RequestURI.getUri() + "/food-classification-service")
			.path("/classify")
			.request()
			.post(Entity.entity(map, MediaType.APPLICATION_JSON));
		return response;
	}

	@POST
	@Path("/{id}/flags")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response flagsDataset(@PathParam("id") String id, Dataset dataset) {
		Response response = ClientBuilder
			.newClient()
			.target(RequestURI.getUri() + "/food-classification-service")
			.path("/flags")
			.request()
			.post(Entity.entity(dataset, MediaType.APPLICATION_JSON));
		return response;
	}

	@POST
	@Path("/{id}/init")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response initDataset(@PathParam("id") String id, Dataset dataset) {
		Response response = ClientBuilder
			.newClient()
			.target(RequestURI.getUri() + "/food-classification-service")
			.path("/init")
			.request()
			.post(Entity.entity(dataset, MediaType.APPLICATION_JSON));
		return response;
	}

	@POST
	@Path("/{id}/adjustment")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public Response adjustmentDataset(@PathParam("id") String id, Dataset dataset) {
		Response response = ClientBuilder
			.newClient()
			.target(RequestURI.getUri() + "/food-classification-service")
			.path("/adjustment")
			.request()
			.post(Entity.entity(dataset, MediaType.APPLICATION_JSON));
		return response;
	}

	@POST
	@Path("/{id}/commit")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void commitDataset() {
	}

	@GET
	@Path("/status")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void getStatusCodes() {
		Map<Integer, String> map = new HashMap<Integer, String>();
		for (Response.Status obj : Response.Status.values()) {
			map.put(obj.getStatusCode(), obj.name());
		}
		int len = 0;
		for (Integer key : map.keySet()) {
			String value = map.get(key);
			if (value.length() > len) {
				len = value.length();
			}
		}
		String format = new StringBuffer()
			.append("%d %-")
			.append(len)
			.append("s")
			.toString();
		String tamrof = new StringBuffer()
			.append("%-")
			.append(len + 4)
			.append("s")
			.toString();
		Integer[][] arr = new Integer[6][18];
		Integer series = 0;
		int i = 0;
		for (Integer key : map.keySet()) {
			if (key / 100 != series) {
				series = key / 100;
				i = 0;
			}
			arr[series][i++] = key;
		}
		for (int j = 0; j < 18; ++j) {
			arr[0][j] = null;
			arr[1][j] = null;
		}
		for (int l = 0; l < 18; ++l) {
			for (int m = 2; m < 6; ++m) {
				Integer key = arr[m][l];
				if (key != null) {
					System.out.printf("[01;%dm" + format + "[00;00m", l % 2 == 0 ? 36 : 35, key, Response.Status.fromStatusCode(key).name());
				} else {
					System.out.printf(tamrof, "");
				}
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
		for (int l = 0; l < 18; ++l) {
			for (int m = 2; m < 6; ++m) {
				Integer key = arr[m][l];
				if (key != null) {
					System.out.printf("[01;%dm" + format + "[00;00m", l % 2 == 0 ? 34 : 31, key, Response.Status.fromStatusCode(key));
				} else {
					System.out.printf(tamrof, "");
				}
			}
			System.out.println();
		}
	}

	public List<FoodItem> getFoodItem(@PathParam("id") Integer id) {
		List<FoodItem> list = new ArrayList<FoodItem>(); // Create list

		try {
			meta = conn.getMetaData(); // Create Oracle DatabaseMetaData object
			logger.error("[01;34mJDBC driver version is " + meta.getDriverVersion() + "[00;00m"); // Retrieve driver information
			String sql = ContentHandler.read("food_item_cnf.sql", getClass());
			PreparedStatement stmt = conn.prepareStatement(sql); // Create PreparedStatement
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				FoodItem food = new FoodItem();
				food.setId(rs.getInt("food_c"));
				food.setName(rs.getString("eng_name"));
				food.setLabel(rs.getString("food_desc"));
				food.setGroup(rs.getString("group_c"));
				food.setSubGroup(rs.getString("canada_food_subgroup_id"));
				food.setCountryCode(rs.getString("country_c"));
				list.add(food);
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}

		logger.error(new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(list));

		return list;
	}

	@GET
	@Path("/group/{groupId}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public List<FoodItem> getFoodListForGroup(@PathParam("groupId") Integer groupId) {
		List<FoodItem> list = new ArrayList<FoodItem>(); // Create list

		try {
			meta = conn.getMetaData(); // Create Oracle DatabaseMetaData object
			logger.error("[01;34mJDBC driver version is " + meta.getDriverVersion() + "[00;00m"); // Retrieve driver information
			String sql = ContentHandler.read("food_table_group_cnf.sql", getClass());
			PreparedStatement stmt = conn.prepareStatement(sql); // Create PreparedStatement
			stmt.setInt(1, groupId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				FoodItem food = new FoodItem();
				food.setId(rs.getInt("food_c"));
				food.setName(rs.getString("eng_name"));
				food.setLabel(rs.getString("food_desc"));
				food.setGroup(rs.getString("group_c"));
				food.setSubGroup(rs.getString("canada_food_subgroup_id"));
				food.setCountryCode(rs.getString("country_c"));
				list.add(food);
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}

		// logger.error(new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(list));

		return list;
	}

	@GET
	@Path("/subgroup/{subgroupId}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public List<FoodItem> getFoodListForsubGroup(@PathParam("subgroupId") Integer subgroupId) {
		List<FoodItem> list = new ArrayList<FoodItem>(); // Create list

		try {
			meta = conn.getMetaData(); // Create Oracle DatabaseMetaData object
			logger.error("[01;34mJDBC driver version is " + meta.getDriverVersion() + "[00;00m"); // Retrieve driver information
			String sql = ContentHandler.read("food_table_subgroup_cnf.sql", getClass());
			PreparedStatement stmt = conn.prepareStatement(sql); // Create PreparedStatement
			stmt.setInt(1, subgroupId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				FoodItem food = new FoodItem();
				food.setId(rs.getInt("food_c"));
				food.setName(rs.getString("eng_name"));
				food.setLabel(rs.getString("food_desc"));
				food.setGroup(rs.getString("group_c"));
				food.setSubGroup(rs.getString("canada_food_subgroup_id"));
				food.setCountryCode(rs.getString("country_c"));
				list.add(food);
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}

		// logger.error(new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(list));

		return list;
	}

	private /* List<CanadaFoodGuideDataset> */ Response doSearchCriteria(CfgFilter search) {
		List<CanadaFoodGuideDataset> list = new ArrayList<CanadaFoodGuideDataset>();

		if (search != null) {
			StringBuffer sb = new StringBuffer(search.getSql());

			logger.error("[01;30m" + search.getDataSource() + "[00;00m");

			sb.append(" WHERE length('this where-clause is an artifact') = 32").append("\n");
			if (search.getDataSource() != null && search.getDataSource().matches("food|recipe")) {
				sb.append("   AND type = ?").append("\n");
			}

			logger.error("[01;30m" + search.getFoodRecipeName() + "[00;00m");

			if (search.getFoodRecipeName() != null && !search.getFoodRecipeName().isEmpty()) {
				sb.append("   AND LOWER(name) LIKE ?").append("\n");
			}

			logger.error("[01;30m" + search.getFoodRecipeCode() + "[00;00m");

			if (search.getFoodRecipeCode() != null && !search.getFoodRecipeCode().isEmpty()) {
				sb.append("   AND code = ? OR CAST(code AS text) LIKE ?").append("\n");
			}

			logger.error("[01;30m" + search.getCnfCode() + "[00;00m");

			if (search.getCnfCode() != null && !search.getCnfCode().isEmpty()) {
				sb.append("   AND cnf_group_code = ?").append("\n");
			}

			logger.error("[01;30m" + search.getSubgroupCode() + "[00;00m");

			if (search.getSubgroupCode() != null && !search.getSubgroupCode().isEmpty()) {
				sb.append("   AND CAST(cfg_code AS text) LIKE ?").append("\n");
			}

			if (search.getCfgTier() != null && !search.getCfgTier().equals(CfgTier.ALL.getCode())) {
				switch (search.getCfgTier()) {
					case 1:
					case 2:
					case 3:
					case 4:
						logger.error("[01;31mCalling all codes with Tier " + search.getCfgTier() + "[00;00m");
						sb.append("   AND LENGTH(CAST(cfg_code AS text)) = 4").append("\n");
						sb.append("   AND CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						break;
					case 12:
					case 13:
					case 14:
					case 23:
					case 24:
					case 34:
						logger.error("[01;31mCalling all codes with Tier " + search.getCfgTier() + "[00;00m");
						sb.append("   AND LENGTH(CAST(cfg_code AS text)) = 4").append("\n");
						sb.append("   AND CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						sb.append("    OR CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						break;
					case 123:
					case 124:
					case 134:
					case 234:
						logger.error("[01;31mCalling all codes with Tier " + search.getCfgTier() + "[00;00m");
						sb.append("   AND LENGTH(CAST(cfg_code AS text)) = 4").append("\n");
						sb.append("   AND CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						sb.append("    OR CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						sb.append("    OR CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						break;
					case 1234:
						logger.error("[01;31mCalling all codes with Tier " + search.getCfgTier() + "[00;00m");
						sb.append("   AND LENGTH(CAST(cfg_code AS text)) = 4").append("\n");
						sb.append("   AND CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						sb.append("    OR CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						sb.append("    OR CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						sb.append("    OR CAST(SUBSTR(CAST(cfg_code AS text), 4, 1) AS integer) = ?").append("\n");
						break;
					case 9:
						logger.error("[01;31mCalling all codes with missing Tier![00;00m");
						sb.append("   AND LENGTH(CAST(cfg_code AS text)) < 4").append("\n");
						break;
				}
			}

			if (search.getRecipe() != null && !search.getRecipe().equals(RecipeRolled.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getRecipe() + "[00;00m");
				switch (search.getRecipe()) {
					case 1:
					case 2:
						sb.append("   AND rolled_up = ?").append("\n");
						break;
					case 3:
						sb.append("   AND (rolled_up = 1 OR rolled_up = 2)").append("\n");
						break;
				}
			}

			boolean notIgnore = false;
			if (search.getContainsAdded() != null) {
				String[] arr = new String[search.getContainsAdded().size()];
				arr = search.getContainsAdded().toArray(arr);
				for (String i : arr) {
					logger.error("[01;32m" + i + "[00;00m");
					if (!i.equals("0")) {
						notIgnore = true;
					}
				}
			}

			Map<String, String> map = null;

			if (search.getContainsAdded() != null && notIgnore) {
				map = new HashMap<String, String>();
				logger.error("[01;32m" + search.getContainsAdded() + "[00;00m");
				String[] arr = new String[search.getContainsAdded().size()];
				arr = search.getContainsAdded().toArray(arr);
				for (String keyValue : arr) {
					StringTokenizer tokenizer = new StringTokenizer(keyValue, "=");
					map.put(tokenizer.nextToken(), tokenizer.nextToken());
					logger.error("[01;32m" + keyValue + "[00;00m");
				}
				logger.error("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");

				Set<String> keys = map.keySet();
				for (String key : keys) {
					switch (ContainsAdded.valueOf(key)) {
						case sodium:
							sb.append("   AND contains_added_sodium      = ? ").append("\n");
							break;
						case sugar:
							sb.append("   AND contains_added_sugar       = ? ").append("\n");
							break;
						case fat:
							sb.append("   AND contains_added_fat         = ? ").append("\n");
							break;
						case transfat:
							sb.append("   AND contains_added_transfat    = ? ").append("\n");
							break;
						case caffeine:
							sb.append("   AND contains_caffeine          = ? ").append("\n");
							break;
						case freeSugars:
							sb.append("   AND contains_free_sugars       = ? ").append("\n");
							break;
						case sugarSubstitute:
							sb.append("   AND contains_sugar_substitutes = ? ").append("\n");
							break;
					}
				}
			}

			if (search.getMissingValues() != null) {
				logger.error("[01;32m" + search.getMissingValues() + "[00;00m");
				logger.error("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(search.getMissingValues()) + "[00;00m");
				for (String name : search.getMissingValues()) {
					switch (Missing.valueOf(name)) {
						case refAmount:
							sb.append("   AND reference_amount_g         IS NULL").append("\n");
							break;
						case cfgServing:
							sb.append("   AND food_guide_serving_g       IS NULL").append("\n");
							break;
						case tier4Serving:
							sb.append("   AND tier_4_serving_g           IS NULL").append("\n");
							break;
						case energy:
							sb.append("   AND energy_kcal                IS NULL").append("\n");
							break;
						case cnfCode:
							sb.append("   AND cnf_group_code             IS NULL").append("\n");
							break;
						case rollUp:
							sb.append("   AND rolled_up                  IS NULL").append("\n");
							break;
						case sodiumPer100g:
							sb.append("   AND sodium_amount_per_100g     IS NULL").append("\n");
							break;
						case sugarPer100g:
							sb.append("   AND sugar_amount_per_100g      IS NULL").append("\n");
							break;
						case fatPer100g:
							sb.append("   AND totalfat_amount_per_100g   IS NULL").append("\n");
							break;
						case transfatPer100g:
							sb.append("   AND transfat_amount_per_100g   IS NULL").append("\n");
							break;
						case satFatPer100g:
							sb.append("   AND satfat_amount_per_100g     IS NULL").append("\n");
							break;
						case addedSodium:
							sb.append("   AND contains_added_sodium      IS NULL").append("\n");
							break;
						case addedSugar:
							sb.append("   AND contains_added_sugar       IS NULL").append("\n");
							break;
						case addedFat:
							sb.append("   AND contains_added_fat         IS NULL").append("\n");
							break;
						case addedTransfat:
							sb.append("   AND contains_added_transfat    IS NULL").append("\n");
							break;
						case caffeine:
							sb.append("   AND contains_caffeine          IS NULL").append("\n");
							break;
						case freeSugars:
							sb.append("   AND contains_free_sugars       IS NULL").append("\n");
							break;
						case sugarSubstitute:
							sb.append("   AND contains_sugar_substitutes IS NULL").append("\n");
							break;
					}
				}
			}

			if (search.getLastUpdateDateFrom() != null && search.getLastUpdateDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getLastUpdateDateTo() != null && search.getLastUpdateDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
				if (search.getLastUpdatedFilter() != null) {
					logger.error("[01;32m" + search.getLastUpdatedFilter() + "[00;00m");
					for (String name : search.getLastUpdatedFilter()) {
						switch (Missing.valueOf(name)) {
							case refAmount:
								sb.append("   AND reference_amount_update_date           BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case cfgServing:
								sb.append("   AND food_guide_update_date                 BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case tier4Serving:
								sb.append("   AND tier_4_serving_update_date             BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case energy:
							case cnfCode:
								break;
							case rollUp:
								sb.append("   AND rolled_up_update_date                  BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case sodiumPer100g:
								sb.append("   AND sodium_imputation_date                 BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case sugarPer100g:
								sb.append("   AND sugar_imputation_date                  BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case fatPer100g:
								sb.append("   AND totalfat_imputation_date               BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case transfatPer100g:
								sb.append("   AND transfat_imputation_date               BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case satFatPer100g:
								sb.append("   AND satfat_imputation_date                 BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case addedSodium:
								sb.append("   AND contains_added_sodium_update_date      BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case addedSugar:
								sb.append("   AND contains_added_sugar_update_date       BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case addedFat:
								sb.append("   AND contains_added_fat_update_date         BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case addedTransfat:
								sb.append("   AND contains_added_transfat_update_date    BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case caffeine:
								sb.append("   AND contains_caffeine_update_date          BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case freeSugars:
								sb.append("   AND contains_free_sugars_update_date       BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
							case sugarSubstitute:
								sb.append("   AND contains_sugar_substitutes_update_date BETWEEN CAST(? AS date) AND CAST(? AS date)").append("\n");
								break;
						}
					}
				}
			}

			if (search.getComments() != null && !search.getComments().isEmpty()) {
				sb.append("   AND LOWER(comments) LIKE ?").append("\n");
			}

			if (search.getCommitDateFrom() != null && search.getCommitDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getCommitDateTo() != null && search.getCommitDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
				sb.append("   AND commit_date                            BETWEEN CAST(? AS date) AND CAST(? AS date)") .append("\n");
			}

			search.setSql(sb.toString());

			try {
				meta = conn.getMetaData(); // Create Oracle DatabaseMetaData object
				logger.error("[01;34mJDBC driver version is " + meta.getDriverVersion() + "[00;00m"); // Retrieve driver information
				PreparedStatement stmt = conn.prepareStatement(search.getSql()); // Create PreparedStatement

				int i = 0; // keeps count of the number of placeholders

				if (search != null) {
					if (search.getDataSource() != null && search.getDataSource().matches("food|recipe")) {
						stmt.setInt(++i, search.getDataSource().equals("food") ? 1 : 2);
					}
					if (search.getFoodRecipeName() != null && !search.getFoodRecipeName().isEmpty()) {
						stmt.setString(++i, new String("%" + search.getFoodRecipeName() + "%").toLowerCase());
					}
					if (search.getFoodRecipeCode() != null && !search.getFoodRecipeCode().isEmpty()) {
						stmt.setInt(++i, Integer.parseInt(search.getFoodRecipeCode()));
						stmt.setString(++i, new String("" + search.getFoodRecipeCode() + "%"));
					}
					if (search.getCnfCode() != null && !search.getCnfCode().isEmpty()) {
						stmt.setInt(++i, Integer.parseInt(search.getCnfCode()));
					}
					if (search.getSubgroupCode() != null && !search.getSubgroupCode().isEmpty()) {
						stmt.setString(++i, new String("" + search.getSubgroupCode() + "%"));
					}
					if (search.getCfgTier() != null && !search.getCfgTier().equals(CfgTier.ALL.getCode())) {
						switch (search.getCfgTier()) {
							case 1:
							case 2:
							case 3:
							case 4:
								stmt.setInt(++i, search.getCfgTier());
								break;
						}
					}
					if (search.getRecipe() != null && !search.getRecipe().equals(RecipeRolled.IGNORE.getCode())) {
						switch (search.getRecipe()) {
							case 1:
							case 2:
								stmt.setInt(++i, search.getRecipe());
								break;
						}
					}

					if (search.getContainsAdded() != null && notIgnore) {
						Set<String> keys = map.keySet();
						for (String key : keys) {
							switch (ContainsAdded.valueOf(key)) {
								case sodium:
								case sugar:
								case fat:
								case transfat:
								case caffeine:
								case freeSugars:
								case sugarSubstitute:
									stmt.setInt(++i, map.get(key).equals("true") ? 1 : 2);
									break;
							}
						}
					}

					if (search.getLastUpdateDateFrom() != null && search.getLastUpdateDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getLastUpdateDateTo() != null && search.getLastUpdateDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
						if (search.getLastUpdatedFilter() != null) {
							logger.error("[01;32m" + search.getLastUpdatedFilter() + "[00;00m");
							for (String name : search.getLastUpdatedFilter()) {
								switch (Missing.valueOf(name)) {
									case refAmount:
									case cfgServing:
									case tier4Serving:
										stmt.setString(++i, search.getLastUpdateDateFrom());
										stmt.setString(++i, search.getLastUpdateDateTo());
										break;
									case energy:
									case cnfCode:
										break;
									case rollUp:
									case sodiumPer100g:
									case sugarPer100g:
									case fatPer100g:
									case transfatPer100g:
									case satFatPer100g:
									case addedSodium:
									case addedSugar:
									case addedFat:
									case addedTransfat:
									case caffeine:
									case freeSugars:
									case sugarSubstitute:
										stmt.setString(++i, search.getLastUpdateDateFrom());
										stmt.setString(++i, search.getLastUpdateDateTo());
										break;
								}
							}
						}
					}

					if (search.getComments() != null && !search.getComments().isEmpty()) {
						stmt.setString(++i, new String("%" + search.getComments() + "%").toLowerCase());
					}

					if (search.getCommitDateFrom() != null && search.getCommitDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getCommitDateTo() != null && search.getCommitDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
						stmt.setString(++i, search.getCommitDateFrom());
						stmt.setString(++i, search.getCommitDateTo());
					}
				}

				logger.error("[01;34mSQL query to follow:\n" + stmt.toString() + "[00;00m");

				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					CanadaFoodGuideDataset foodItem = new CanadaFoodGuideDataset();

					foodItem.setType(rs. getInt                               ("type") == 1 ? "food" : "recipe"          );
					foodItem.setCode(Integer.parseInt(rs.getString            ("code")                                  ));
					foodItem.setName(rs.getString                             ("name")                                   );
					if (rs.getString("cnf_group_code") != null)
						foodItem.setCnfGroupCode(rs.getInt                    ("cnf_group_code")                         );
					if (rs.getString("cfg_code") != null)
						foodItem.setCfgCode(rs.getInt                         ("cfg_code")                               );
					foodItem.setCommitDate(rs.getDate                         ("cfg_code_update_date")                   );
					if (rs.getString("energy_kcal") != null)
						foodItem.setEnergyKcal(rs.getDouble                   ("energy_kcal")                            );
					if (rs.getString("sodium_amount_per_100g") != null)
						foodItem.setSodiumAmountPer100g(rs.getDouble          ("sodium_amount_per_100g")                 );
					foodItem.setSodiumImputationReference(rs.getString        ("sodium_imputation_reference")            );
					foodItem.setSodiumImputationDate(rs.getDate               ("sodium_imputation_date")                 );
					if (rs.getString("sugar_amount_per_100g") != null)
						foodItem.setSugarAmountPer100g(rs.getDouble           ("sugar_amount_per_100g")                  );
					foodItem.setSugarImputationReference(rs.getString         ("sugar_imputation_reference")             );
					foodItem.setSugarImputationDate(rs.getDate                ("sugar_imputation_date")                  );
					if (rs.getString("transfat_amount_per_100g") != null)
						foodItem.setTransfatAmountPer100g(rs.getDouble        ("transfat_amount_per_100g")               );
					foodItem.setTransfatImputationReference(rs.getString      ("transfat_imputation_reference")          );
					foodItem.setTransfatImputationDate(rs.getDate             ("transfat_imputation_date")               );
					if (rs.getString("satfat_amount_per_100g") != null)
						foodItem.setSatfatAmountPer100g(rs.getDouble          ("satfat_amount_per_100g")                 );
					foodItem.setSatfatImputationReference(rs.getString        ("satfat_imputation_reference")            );
					foodItem.setSatfatImputationDate(rs.getDate               ("satfat_imputation_date")                 );
					if (rs.getString("totalfat_amount_per_100g") != null)
						foodItem.setTotalfatAmountPer100g(rs.getDouble        ("totalfat_amount_per_100g")               );
					foodItem.setTotalfatImputationReference(rs.getString      ("totalfat_imputation_reference")          );
					foodItem.setTotalfatImputationDate(rs.getDate             ("totalfat_imputation_date")               );
					if (rs.getString("contains_added_sodium") != null)
						foodItem.setContainsAddedSodium(rs.getBoolean         ("contains_added_sodium")                  );
					foodItem.setContainsAddedSodiumUpdateDate(rs.getDate      ("contains_added_sodium_update_date")      );
					if (rs.getString("contains_added_sugar") != null)
						foodItem.setContainsAddedSugar(rs.getBoolean          ("contains_added_sugar")                   );
					foodItem.setContainsAddedSugarUpdateDate(rs.getDate       ("contains_added_sugar_update_date")       );
					if (rs.getString("contains_free_sugars") != null)
						foodItem.setContainsFreeSugars(rs.getBoolean          ("contains_free_sugars")                   );
					foodItem.setContainsFreeSugarsUpdateDate(rs.getDate       ("contains_free_sugars_update_date")       );
					if (rs.getString("contains_added_fat") != null)
						foodItem.setContainsAddedFat(rs.getBoolean            ("contains_added_fat")                     );
					foodItem.setContainsAddedFatUpdateDate(rs.getDate         ("contains_added_fat_update_date")         );
					if (rs.getString("contains_added_transfat") != null)
						foodItem.setContainsAddedTransfat(rs.getBoolean       ("contains_added_transfat")                );
					foodItem.setContainsAddedTransfatUpdateDate(rs.getDate    ("contains_added_transfat_update_date")    );
					if (rs.getString("contains_caffeine") != null)
						foodItem.setContainsCaffeine(rs.getBoolean            ("contains_caffeine")                      );
					foodItem.setContainsCaffeineUpdateDate(rs.getDate         ("contains_caffeine_update_date")          );
					if (rs.getString("contains_sugar_substitutes") != null)
						foodItem.setContainsSugarSubstitutes(rs.getBoolean    ("contains_sugar_substitutes")             );
					foodItem.setContainsSugarSubstitutesUpdateDate(rs.getDate ("contains_sugar_substitutes_update_date") );
					if (rs.getString("reference_amount_g") != null)
						foodItem.setReferenceAmountG(rs.getDouble             ("reference_amount_g")                     );
					foodItem.setReferenceAmountMeasure(rs.getString           ("reference_amount_measure")               );
					foodItem.setReferenceAmountUpdateDate(rs.getDate          ("reference_amount_update_date")           );
					if (rs.getString("food_guide_serving_g") != null)
						foodItem.setFoodGuideServingG(rs.getDouble            ("food_guide_serving_g")                   );
					foodItem.setFoodGuideServingMeasure(rs.getString          ("food_guide_serving_measure")             );
					foodItem.setFoodGuideUpdateDate(rs.getDate                ("food_guide_update_date")                 );
					if (rs.getString("tier_4_serving_g") != null)
						foodItem.setTier4ServingG(rs.getDouble                ("tier_4_serving_g")                       );
					foodItem.setTier4ServingMeasure(rs.getString              ("tier_4_serving_measure")                 );
					foodItem.setTier4ServingUpdateDate(rs.getDate             ("tier_4_serving_update_date")             );
					if (rs.getString("rolled_up") != null)
						foodItem.setRolledUp(rs.getBoolean                    ("rolled_up")                              );
					foodItem.setRolledUpUpdateDate(rs.getDate                 ("rolled_up_update_date")                  );
					if (rs.getString("apply_small_ra_adjustment") != null)
						foodItem.setOverrideSmallRaAdjustment(rs.getBoolean   ("apply_small_ra_adjustment")              );
					if (rs.getString("replacement_code") != null)
						foodItem.setReplacementCode(rs.getInt                 ("replacement_code")                       );
					foodItem.setCommitDate(rs.getDate                         ("commit_date")                            );
					foodItem.setComments(rs.getString                         ("comments")                               );

					list.add(foodItem);
				}
			} catch(SQLException e) {
				e.printStackTrace();
			}

			// logger.error(new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(list));
		}

		return Response.status(Response.Status.OK)
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
			.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
			.header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1209600")
			.entity(list).build();
	}

	private static <T> List<T> castList(Object obj, Class<T> clazz) {
		List<T> result = new ArrayList<T>();
		if (obj instanceof List<?>) {
			for (Object o : (List<?>)obj) {
				result.add(clazz.cast(o));
			}
			return result;
		}
		return null;
	}
}
