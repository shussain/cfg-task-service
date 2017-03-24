package ca.gc.ip346.classification.resource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.annotation.JacksonFeatures;
import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;

import ca.gc.ip346.classification.model.Added;
import ca.gc.ip346.classification.model.CanadaFoodGuideDataset;
import ca.gc.ip346.classification.model.CfgTier;
import ca.gc.ip346.classification.model.ContainsAdded;
import ca.gc.ip346.classification.model.FoodItem;
import ca.gc.ip346.classification.model.Missing;
import ca.gc.ip346.classification.model.RecipeRolled;
// import ca.gc.ip346.classification.model.NewAndImprovedFoodItem;
import ca.gc.ip346.classification.model.CfgFilter;
import ca.gc.ip346.util.DBConnection;

@Path("/datasets")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class FoodsResource {
	private static final Logger logger = LogManager.getLogger(FoodsResource.class);

	Connection conn       = null;
	DatabaseMetaData meta = null;

	public FoodsResource() {
		try {
			// Context initCtx = new InitialContext();
			// Context envCtx  = (Context)initCtx.lookup("java:comp/env");
			// DataSource ds   = (DataSource)envCtx.lookup("jdbc/FoodDB");
			// conn = ds.getConnection();
			conn = DBConnection.getConnections();
		} catch(NamingException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@GET
	@Path("/search")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public List<CanadaFoodGuideDataset> getFoodList(@BeanParam CfgFilter search) {
		String sql = ContentHandler.read("canada_food_guide_dataset.sql", getClass());
		search.setSql(sql);
		return doSearchCriteria(search);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void saveDataset() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void getDatasets() {
	}

	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void deleteDataset() {
	}

	@POST
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void updateDataset() {
	}

	@POST
	@Path("/{id}/classify")
	@Produces(MediaType.APPLICATION_JSON)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void classifyDataset() {
	}

	@POST
	@Path("/{id}/commit")
	@Produces(MediaType.APPLICATION_JSON)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
	public void commitDataset() {
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@JacksonFeatures(serializationEnable = {SerializationFeature.INDENT_OUTPUT})
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
	@Produces(MediaType.APPLICATION_JSON)
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
	@Produces(MediaType.APPLICATION_JSON)
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

	private List<CanadaFoodGuideDataset> doSearchCriteria(CfgFilter search) {
		List<CanadaFoodGuideDataset> list = new ArrayList<CanadaFoodGuideDataset>();

		if (search != null) {
			StringBuffer sb = new StringBuffer(search.getSql());

			sb.append(" WHERE length('this where-clause is an artifact') = 32 ").append("\n");
			if (!search.getDataSource().equals("0")) {
				sb.append("   AND type = ?").append("\n");
			}
			if (!search.getFoodRecipeName().                                         isEmpty ()) {
				sb.append("   AND LOWER(name) LIKE ?").append("\n");
			}
			if (!search.getFoodRecipeCode().                                         isEmpty ()) {
				sb.append("   AND code = ? OR CAST(code AS text) LIKE ?").append("\n");
			}
			if (!search.getCnfCode().                                                isEmpty ()) {
				sb.append("   AND cnf_group_code = ?").append("\n");
			}
			if (!search.getSubgroupCode().                                           isEmpty ()) {
				sb.append("   AND CAST(cfg_code AS text) LIKE ?").append("\n");
			}

			if (!search.getCfgTier().equals(CfgTier.ALL.getCode())) {
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
			if (search.getRecipe()           != null && !search.getRecipe().           equals (RecipeRolled.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getRecipe() + "[00;00m");
				sb.append("   AND rolled_up = ?").append("\n");
			}
			if (search.getSodium()           != null && !search.getSodium().           equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getSodium() + "[00;00m");
				sb.append("   AND contains_added_sodium = ?").append("\n");
			}
			if (search.getSugar()            != null && !search.getSugar().            equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getSugar() + "[00;00m");
				sb.append("   AND contains_added_sugar = ?").append("\n");
			}
			if (search.getFat()              != null && !search.getFat().              equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getFat() + "[00;00m");
				sb.append("   AND contains_added_fat = ?").append("\n");
			}
			if (search.getTransfat()         != null && !search.getTransfat().         equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getTransfat() + "[00;00m");
				sb.append("   AND contains_added_transfat = ?").append("\n");
			}
			if (search.getCaffeine()         != null && !search.getCaffeine().         equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getCaffeine() + "[00;00m");
				sb.append("   AND contains_caffeine = ?").append("\n");
			}
			if (search.getFreeSugars()       != null && !search.getFreeSugars().       equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getFreeSugars() + "[00;00m");
				sb.append("   AND contains_free_sugars = ?").append("\n");
			}
			if (search.getSugarSubstitutes() != null && !search.getSugarSubstitutes(). equals (Added.IGNORE.getCode())) {
				logger.error("[01;32m" + search.getSugarSubstitutes() + "[00;00m");
				sb.append("   AND contains_sugar_substitutes = ?").append("\n");
			}

			boolean notIgnore = false;
			String[] arr = new String[search.getContainsAdded().size()];
			arr = search.getContainsAdded().toArray(arr);
			for (String i : arr) {
				logger.error("[01;32m" + i + "[00;00m");
				if (!i.equals("0")) {
					notIgnore = true;
				}
			}

			Map<String, String> map = null;

			if (search.getContainsAdded() != null && notIgnore) {
				map = new HashMap<String, String>();
				logger.error("[01;32m" + search.getContainsAdded() + "[00;00m");
				for (String keyValue : search.getContainsAdded()) {
					StringTokenizer tokenizer = new StringTokenizer(keyValue, "=");
					map.put(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, tokenizer.nextToken()),tokenizer.nextToken());
					logger.error("[01;32m" + keyValue + "[00;00m");
				}
				logger.error("\n[01;32m" + new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(map) + "[00;00m");

				Set<String> keys = map.keySet();
				for (String key : keys) {
					switch (ContainsAdded.valueOf(key)) {
						case sodium:
							sb.append("   AND contains_added_sodium = ?")      .append ("\n");
							break;
						case sugar:
							sb.append("   AND contains_added_sugar = ?")       .append ("\n");
							break;
						case fat:
							sb.append("   AND contains_added_fat = ?")         .append ("\n");
							break;
						case transfat:
							sb.append("   AND contains_added_transfat = ?")    .append ("\n");
							break;
						case caffeine:
							sb.append("   AND contains_caffeine = ?")          .append ("\n");
							break;
						case freeSugars:
							sb.append("   AND contains_free_sugars = ?")       .append ("\n");
							break;
						case sugarSubstitutes:
							sb.append("   AND contains_sugar_substitutes = ?") .append ("\n");
							break;
					}
				}
			}

			if (search.getMissingValues() != null) {
				logger.error("[01;32m" + search.getMissingValues() + "[00;00m");
				for (String name : search.getMissingValues()) {
					switch (Missing.valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, name))) {
						case referenceAmount:
							sb.append("   AND reference_amount_g = NULL").append("\n");
							sb.append("    OR reference_amount_g = 0").append("\n");
						case cfgServing:
							sb.append("   AND food_guide_serving_g = NULL").append("\n");
							sb.append("    OR food_guide_serving_g = 0").append("\n");
						case tier4Serving:
							sb.append("   AND tier_4_serving_g = NULL").append("\n");
							sb.append("    OR tier_4_serving_g = 0").append("\n");
						case energyValue:
							sb.append("   AND energy_kcal = NULL").append("\n");
							sb.append("    OR energy_kcal = 0").append("\n");
						case cnfCode:
							sb.append("   AND cnf_group_code = NULL").append("\n");
							sb.append("    OR cnf_group_code = 0").append("\n");
						case recipeRolledUpDown:
							sb.append("   AND rolled_up = NULL").append("\n");
							sb.append("    OR rolled_up = 0").append("\n");
						case sodiumValue:
							sb.append("   AND sodium_amount_per_100g = NULL").append("\n");
							sb.append("    OR sodium_amount_per_100g = 0").append("\n");
						case sugarValue:
							sb.append("   AND sugar_amount_per_100g = NULL").append("\n");
							sb.append("    OR sugar_amount_per_100g = 0").append("\n");
						case fatValue:
							sb.append("   AND totalfat_amount_per_100g = NULL").append("\n");
							sb.append("    OR totalfat_amount_per_100g = 0").append("\n");
						case transfatValue:
							sb.append("   AND transfat_amount_per_100g = NULL").append("\n");
							sb.append("    OR transfat_amount_per_100g = 0").append("\n");
						case satfatValue:
							sb.append("   AND satfat_amount_per_100g = NULL").append("\n");
							sb.append("    OR satfat_amount_per_100g = 0").append("\n");
						case addedSodium:
							sb.append("   AND contains_added_sodium = NULL").append("\n");
							sb.append("    OR contains_added_sodium = 0").append("\n");
						case addedSugar:
							sb.append("   AND contains_added_sugar = NULL").append("\n");
							sb.append("    OR contains_added_sugar = 0").append("\n");
						case addedFat:
							sb.append("   AND contains_added_fat = NULL").append("\n");
							sb.append("    OR contains_added_fat = 0").append("\n");
						case addedTransfat:
							sb.append("   AND contains_added_transfat = NULL").append("\n");
							sb.append("    OR contains_added_transfat = 0").append("\n");
						case caffeine:
							sb.append("   AND contains_caffeine = NULL").append("\n");
							sb.append("    OR contains_caffeine = 0").append("\n");
						case freeSugars:
							sb.append("   AND contains_free_sugars = NULL").append("\n");
							sb.append("    OR contains_free_sugars = 0").append("\n");
						case sugarSubstitutes:
							sb.append("   AND contains_sugar_substitutes = NULL").append("\n");
							sb.append("    OR contains_sugar_substitutes = 0").append("\n");
					}
				}
			}

			if (search.getLastUpdateDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getLastUpdateDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
				if (search.getLastUpdatedFilter() != null) {
					logger.error("[01;32m" + search.getLastUpdatedFilter() + "[00;00m");
					for (String name : search.getLastUpdatedFilter()) {
						switch (Missing.valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, name))) {
							case referenceAmount:
								sb.append("   AND reference_amount_update_date BETWEEN ? AND ?")           .append("\n");
								break;
							case cfgServing:
								sb.append("   AND food_guide_update_date BETWEEN ? AND ?")                 .append("\n");
								break;
							case tier4Serving:
								sb.append("   AND tier_4_serving_update_date BETWEEN ? AND ?")             .append("\n");
								break;
							case energyValue:
							case cnfCode:
								break;
							case recipeRolledUpDown:
								sb.append("   AND rolled_up_update_date BETWEEN ? AND ?")                  .append("\n");
								break;
							case sodiumValue:
								sb.append("   AND sodium_imputation_date BETWEEN ? AND ?")                 .append("\n");
								break;
							case sugarValue:
								sb.append("   AND sugar_imputation_date BETWEEN ? AND ?")                  .append("\n");
								break;
							case fatValue:
								sb.append("   AND totalfat_imputation_date BETWEEN ? AND ?")               .append("\n");
								break;
							case transfatValue:
								sb.append("   AND transfat_imputation_date BETWEEN ? AND ?")               .append("\n");
								break;
							case satfatValue:
								sb.append("   AND satfat_imputation_date BETWEEN ? AND ?")                 .append("\n");
								break;
							case addedSodium:
								sb.append("   AND contains_added_sodium_update_date BETWEEN ? AND ?")      .append("\n");
								break;
							case addedSugar:
								sb.append("   AND contains_added_sugar_update_date BETWEEN ? AND ?")       .append("\n");
								break;
							case addedFat:
								sb.append("   AND contains_added_fat_update_date BETWEEN ? AND ?")         .append("\n");
								break;
							case addedTransfat:
								sb.append("   AND contains_added_transfat_update_date BETWEEN ? AND ?")    .append("\n");
								break;
							case caffeine:
								sb.append("   AND contains_caffeine_update_date BETWEEN ? AND ?")          .append("\n");
								break;
							case freeSugars:
								sb.append("   AND contains_free_sugars_update_date BETWEEN ? AND ?")       .append("\n");
								break;
							case sugarSubstitutes:
								sb.append("   AND contains_sugar_substitutes_update_date BETWEEN ? AND ?") .append("\n");
								break;
						}
					}
				}
			}

			if (!search.getComments().isEmpty()) {
				sb.append("   AND LOWER(comments) LIKE ?").append("\n");
			}

			if (search.getCommitDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getCommitDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
				sb.append("   AND commit_date BETWEEN ? AND ?") .append("\n");
			}

			if (search.getReferenceAmountMissing()       != null && !search.getReferenceAmountMissing().       isEmpty ())  {
				logger.error("[01;31m" + search.getReferenceAmountMissing() + "[00;00m");
				sb.append("   AND reference_amount_g = NULL").append("\n");
				sb.append("    OR reference_amount_g = 0").append("\n");
			}
			if (search.getCfgServingMissing()            != null && !search.getCfgServingMissing().            isEmpty ())  {
				logger.error("[01;31m" + search.getCfgServingMissing() + "[00;00m");
				sb.append("   AND food_guide_serving_g = NULL").append("\n");
				sb.append("    OR food_guide_serving_g = 0").append("\n");
			}
			if (search.getTier4ServingMissing()          != null && !search.getTier4ServingMissing().          isEmpty ())  {
				logger.error("[01;31m" + search.getTier4ServingMissing() + "[00;00m");
				sb.append("   AND tier_4_serving_g = NULL").append("\n");
				sb.append("    OR tier_4_serving_g = 0").append("\n");
			}
			if (search.getEnergyValueMissing()           != null && !search.getEnergyValueMissing().           isEmpty ())  {
				logger.error("[01;31m" + search.getEnergyValueMissing() + "[00;00m");
				sb.append("   AND energy_kcal = NULL").append("\n");
				sb.append("    OR energy_kcal = 0").append("\n");
			}
			if (search.getCnfCodeMissing()               != null && !search.getCnfCodeMissing().               isEmpty ())  {
				logger.error("[01;31m" + search.getCnfCodeMissing() + "[00;00m");
				sb.append("   AND cnf_group_code = NULL").append("\n");
				sb.append("    OR cnf_group_code = 0").append("\n");
			}
			if (search.getRecipeRolledUpDownMissing()    != null && !search.getRecipeRolledUpDownMissing().    isEmpty ())  {
				logger.error("[01;31m" + search.getRecipeRolledUpDownMissing() + "[00;00m");
				sb.append("   AND rolled_up = NULL").append("\n");
				sb.append("    OR rolled_up = 0").append("\n");
			}
			if (search.getSodiumValueMissing()           != null && !search.getSodiumValueMissing().           isEmpty ())  {
				logger.error("[01;31m" + search.getSodiumValueMissing() + "[00;00m");
				sb.append("   AND sodium_amount_per_100g = NULL").append("\n");
				sb.append("    OR sodium_amount_per_100g = 0").append("\n");
			}
			if (search.getSugarValueMissing()            != null && !search.getSugarValueMissing().            isEmpty ())  {
				logger.error("[01;31m" + search.getSugarValueMissing() + "[00;00m");
				sb.append("   AND sugar_amount_per_100g = NULL").append("\n");
				sb.append("    OR sugar_amount_per_100g = 0").append("\n");
			}
			if (search.getFatValueMissing()              != null && !search.getFatValueMissing().              isEmpty ())  {
				logger.error("[01;31m" + search.getFatValueMissing() + "[00;00m");
				sb.append("   AND totalfat_amount_per_100g = NULL").append("\n");
				sb.append("    OR totalfat_amount_per_100g = 0").append("\n");
			}
			if (search.getTransfatValueMissing()         != null && !search.getTransfatValueMissing().         isEmpty ())  {
				logger.error("[01;31m" + search.getTransfatValueMissing() + "[00;00m");
				sb.append("   AND transfat_amount_per_100g = NULL").append("\n");
				sb.append("    OR transfat_amount_per_100g = 0").append("\n");
			}
			if (search.getSatfatValueMissing()           != null && !search.getSatfatValueMissing().           isEmpty ())  {
				logger.error("[01;31m" + search.getSatfatValueMissing() + "[00;00m");
				sb.append("   AND satfat_amount_per_100g = NULL").append("\n");
				sb.append("    OR satfat_amount_per_100g = 0").append("\n");
			}
			if (search.getAddedSodiumMissing()           != null && !search.getAddedSodiumMissing().           isEmpty ())  {
				logger.error("[01;31m" + search.getAddedSodiumMissing() + "[00;00m");
				sb.append("   AND contains_added_sodium = NULL").append("\n");
				sb.append("    OR contains_added_sodium = 0").append("\n");
			}
			if (search.getAddedSugarMissing()            != null && !search.getAddedSugarMissing().            isEmpty ())  {
				logger.error("[01;31m" + search.getAddedSugarMissing() + "[00;00m");
				sb.append("   AND contains_added_sugar = NULL").append("\n");
				sb.append("    OR contains_added_sugar = 0").append("\n");
			}
			if (search.getAddedFatMissing()         != null && !search.getAddedFatMissing().                   isEmpty ())  {
				logger.error("[01;31m" + search.getAddedFatMissing() + "[00;00m");
				sb.append("   AND contains_added_fat = NULL").append("\n");
				sb.append("    OR contains_added_fat = 0").append("\n");
			}
			if (search.getAddedTransfatMissing()         != null && !search.getAddedTransfatMissing().         isEmpty ())  {
				logger.error("[01;31m" + search.getAddedTransfatMissing() + "[00;00m");
				sb.append("   AND contains_added_transfat = NULL").append("\n");
				sb.append("    OR contains_added_transfat = 0").append("\n");
			}
			if (search.getAddedCaffeineMissing()         != null && !search.getAddedCaffeineMissing().         isEmpty ())  {
				logger.error("[01;31m" + search.getAddedCaffeineMissing() + "[00;00m");
				sb.append("   AND contains_caffeine = NULL").append("\n");
				sb.append("    OR contains_caffeine = 0").append("\n");
			}
			if (search.getAddedFreeSugarsMissing()       != null && !search.getAddedFreeSugarsMissing().       isEmpty ())  {
				logger.error("[01;31m" + search.getAddedFreeSugarsMissing() + "[00;00m");
				sb.append("   AND contains_free_sugars = NULL").append("\n");
				sb.append("    OR contains_free_sugars = 0").append("\n");
			}
			if (search.getAddedSugarSubstitutesMissing() != null && !search.getAddedSugarSubstitutesMissing(). isEmpty ())  {
				logger.error("[01;31m" + search.getAddedSugarSubstitutesMissing() + "[00;00m");
				sb.append("   AND contains_sugar_substitutes = NULL").append("\n");
				sb.append("    OR contains_sugar_substitutes = 0").append("\n");
			}

			if (!search.getLastUpdateDateFrom().isEmpty() && !search.getLastUpdateDateTo().isEmpty()) {
				if (search.getReferenceAmountLastUpdated()       != null && !search.getReferenceAmountLastUpdated().       isEmpty ())  {
					logger.error("[01;30m" + search.getReferenceAmountLastUpdated() + "[00;00m");
				}
				if (search.getCfgServingLastUpdated()            != null && !search.getCfgServingLastUpdated().            isEmpty ())  {
					logger.error("[01;30m" + search.getCfgServingLastUpdated() + "[00;00m");
				}
				if (search.getTier4ServingLastUpdated()          != null && !search.getTier4ServingLastUpdated().          isEmpty ())  {
					logger.error("[01;30m" + search.getTier4ServingLastUpdated() + "[00;00m");
				}
				if (search.getEnergyValueLastUpdated()           != null && !search.getEnergyValueLastUpdated().           isEmpty ())  {
					logger.error("[01;30m" + search.getEnergyValueLastUpdated() + "[00;00m");
				}
				if (search.getCnfCodeLastUpdated()               != null && !search.getCnfCodeLastUpdated().               isEmpty ())  {
					logger.error("[01;30m" + search.getCnfCodeLastUpdated() + "[00;00m");
				}
				if (search.getRecipeRolledUpDownLastUpdated()    != null && !search.getRecipeRolledUpDownLastUpdated().    isEmpty ())  {
					logger.error("[01;30m" + search.getRecipeRolledUpDownLastUpdated() + "[00;00m");
				}
				if (search.getSodiumValueLastUpdated()           != null && !search.getSodiumValueLastUpdated().           isEmpty ())  {
					logger.error("[01;30m" + search.getSodiumValueLastUpdated() + "[00;00m");
				}
				if (search.getSugarValueLastUpdated()            != null && !search.getSugarValueLastUpdated().            isEmpty ())  {
					logger.error("[01;30m" + search.getSugarValueLastUpdated() + "[00;00m");
				}
				if (search.getFatValueLastUpdated()              != null && !search.getFatValueLastUpdated().              isEmpty ())  {
					logger.error("[01;30m" + search.getFatValueLastUpdated() + "[00;00m");
				}
				if (search.getTransfatValueLastUpdated()         != null && !search.getTransfatValueLastUpdated().         isEmpty ())  {
					logger.error("[01;30m" + search.getTransfatValueLastUpdated() + "[00;00m");
				}
				if (search.getSatfatValueLastUpdated()           != null && !search.getSatfatValueLastUpdated().           isEmpty ())  {
					logger.error("[01;30m" + search.getSatfatValueLastUpdated() + "[00;00m");
				}
				if (search.getAddedSodiumLastUpdated()           != null && !search.getAddedSodiumLastUpdated().           isEmpty ())  {
					logger.error("[01;30m" + search.getAddedSodiumLastUpdated() + "[00;00m");
				}
				if (search.getAddedSugarLastUpdated()            != null && !search.getAddedSugarLastUpdated().            isEmpty ())  {
					logger.error("[01;30m" + search.getAddedSugarLastUpdated() + "[00;00m");
				}
				if (search.getAddedTransfatLastUpdated()         != null && !search.getAddedTransfatLastUpdated().         isEmpty ())  {
					logger.error("[01;30m" + search.getAddedTransfatLastUpdated() + "[00;00m");
				}
				if (search.getAddedCaffeineLastUpdated()         != null && !search.getAddedCaffeineLastUpdated().         isEmpty ())  {
					logger.error("[01;30m" + search.getAddedCaffeineLastUpdated() + "[00;00m");
				}
				if (search.getAddedFreeSugarsLastUpdated()       != null && !search.getAddedFreeSugarsLastUpdated().       isEmpty ())  {
					logger.error("[01;30m" + search.getAddedFreeSugarsLastUpdated() + "[00;00m");
				}
				if (search.getAddedSugarSubstitutesLastUpdated() != null && !search.getAddedSugarSubstitutesLastUpdated(). isEmpty ())  {
					logger.error("[01;30m" + search.getAddedSugarSubstitutesLastUpdated() + "[00;00m");
				}
			}

			logger.error("[01;34mSQL query to follow:\n" + sb + "[00;00m");

			search.setSql(sb.toString());

			try {
				meta = conn.getMetaData(); // Create Oracle DatabaseMetaData object
				logger.error("[01;34mJDBC driver version is " + meta.getDriverVersion() + "[00;00m"); // Retrieve driver information
				PreparedStatement stmt = conn.prepareStatement(search.getSql()); // Create PreparedStatement

				int i = 0; // keeps count of the number of placeholders

				if (search != null) {
					if (!search.getDataSource().equals("0")) {
						stmt.setInt(++i, Integer.parseInt(search.getDataSource()));
					}
					if (!search.getFoodRecipeName().isEmpty()) {
						stmt.setString(++i, new String("%" + search.getFoodRecipeName() + "%").toLowerCase());
					}
					if (!search.getFoodRecipeCode().isEmpty()) {
						stmt.setInt(++i, Integer.parseInt(search.getFoodRecipeCode()));
						stmt.setString(++i, new String("" + search.getFoodRecipeCode() + "%"));
					}
					if (!search.getCnfCode().isEmpty()) {
						stmt.setInt(++i, Integer.parseInt(search.getCnfCode()));
					}
					if (!search.getSubgroupCode().isEmpty()) {
						stmt.setString(++i, new String("" + search.getSubgroupCode() + "%"));
					}
					if (!search.getCfgTier().equals(CfgTier.ALL.getCode())) {
						switch (search.getCfgTier()) {
							case 1:
							case 2:
							case 3:
							case 4:
								stmt.setInt(++i, search.getCfgTier());
								break;
						}
					}
					if (search.getRecipe()           != null && !search.getRecipe().           equals (RecipeRolled.IGNORE.getCode())) {
						stmt.setInt(++i, search.getRecipe());
					}
					if (search.getSodium()           != null && !search.getSodium().           equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getSodium());
					}
					if (search.getSugar()            != null && !search.getSugar().            equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getSugar());
					}
					if (search.getFat()              != null && !search.getFat().              equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getFat());
					}
					if (search.getTransfat()         != null && !search.getTransfat().         equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getTransfat());
					}
					if (search.getCaffeine()         != null && !search.getCaffeine().         equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getCaffeine());
					}
					if (search.getFreeSugars()       != null && !search.getFreeSugars().       equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getFreeSugars());
					}
					if (search.getSugarSubstitutes() != null && !search.getSugarSubstitutes(). equals (Added.IGNORE.getCode())) {
						stmt.setInt(++i, search.getSugarSubstitutes());
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
								case sugarSubstitutes:
									stmt.setInt(++i, map.get(key).equals("true") ? 1 : 2);
									break;
							}
						}
					}

					if (search.getLastUpdateDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getLastUpdateDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
						if (search.getLastUpdatedFilter() != null) {
							logger.error("[01;32m" + search.getLastUpdatedFilter() + "[00;00m");
							for (String name : search.getLastUpdatedFilter()) {
								switch (Missing.valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, name))) {
									case referenceAmount:
									case cfgServing:
									case tier4Serving:
										stmt.setString(++i, search.getLastUpdateDateFrom());
										stmt.setString(++i, search.getLastUpdateDateTo());
										break;
									case energyValue:
									case cnfCode:
										break;
									case recipeRolledUpDown:
									case sodiumValue:
									case sugarValue:
									case fatValue:
									case transfatValue:
									case satfatValue:
									case addedSodium:
									case addedSugar:
									case addedFat:
									case addedTransfat:
									case caffeine:
									case freeSugars:
									case sugarSubstitutes:
										stmt.setString(++i, search.getLastUpdateDateFrom());
										stmt.setString(++i, search.getLastUpdateDateTo());
										break;
								}
							}
						}
					}

					if (!search.getComments().isEmpty()) {
						stmt.setString(++i, search.getComments());
					}

					if (search.getCommitDateFrom().matches("\\d{4}-\\d{2}-\\d{2}") && search.getCommitDateTo().matches("\\d{4}-\\d{2}-\\d{2}")) {
						stmt.setString(++i, search.getLastUpdateDateFrom());
						stmt.setString(++i, search.getLastUpdateDateTo());
					}
				}

				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					CanadaFoodGuideDataset foodItem = new CanadaFoodGuideDataset();

					foodItem.setType(Integer.parseInt(rs.getString("type")));
					foodItem.setCode(Integer.parseInt(rs.getString("code")));
					foodItem.setName(rs.getString("name"));
					foodItem.setCnfGroupCode(rs.getInt("cnf_group_code"));
					foodItem.setCfgCode(rs.getInt("cfg_code"));
					foodItem.setCommitDate(rs.getDate("commit_date"));
					foodItem.setEnergyKcal(rs.getDouble("energy_kcal"));
					foodItem.setSodiumAmountPer100g(rs.getDouble("sodium_amount_per_100g"));
					foodItem.setSodiumImputationReference(rs.getString("sodium_imputation_reference"));
					foodItem.setSodiumImputationDate(rs.getDate("sodium_imputation_date"));
					foodItem.setSugarAmountPer100g(rs.getDouble("sugar_amount_per_100g"));
					foodItem.setSugarImputationReference(rs.getString("sugar_imputation_reference"));
					foodItem.setSugarImputationDate(rs.getDate("sugar_imputation_date"));
					foodItem.setTransfatAmountPer100g(rs.getDouble("transfat_amount_per_100g"));
					foodItem.setTransfatImputationReference(rs.getString("transfat_imputation_reference"));
					foodItem.setTransfatImputationDate(rs.getDate("transfat_imputation_date"));
					foodItem.setSatfatAmountPer100g(rs.getDouble("satfat_amount_per_100g"));
					foodItem.setSatfatImputationReference(rs.getString("satfat_imputation_reference"));
					foodItem.setSatfatImputationDate(rs.getDate("satfat_imputation_date"));
					foodItem.setTotalfatAmountPer100g(rs.getDouble("totalfat_amount_per_100g"));
					foodItem.setTotalfatImputationReference(rs.getString("totalfat_imputation_reference"));
					foodItem.setTotalfatImputationDate(rs.getDate("totalfat_imputation_date"));
					foodItem.setContainsAddedSodium(rs.getInt("contains_added_sodium"));
					foodItem.setContainsAddedSodiumUpdateDate(rs.getDate("contains_added_sodium_update_date"));
					foodItem.setContainsAddedSugar(rs.getInt("contains_added_sugar"));
					foodItem.setContainsAddedSugarUpdateDate(rs.getDate("contains_added_sugar_update_date"));
					foodItem.setContainsFreeSugars(rs.getInt("contains_free_sugars"));
					foodItem.setContainsFreeSugarsUpdateDate(rs.getDate("contains_free_sugars_update_date"));
					foodItem.setContainsAddedFat(rs.getInt("contains_added_fat"));
					foodItem.setContainsAddedFatUpdateDate(rs.getDate("contains_added_fat_update_date"));
					foodItem.setContainsAddedTransfat(rs.getInt("contains_added_transfat"));
					foodItem.setContainsAddedTransfatUpdateDate(rs.getDate("contains_added_transfat_update_date"));
					foodItem.setContainsCaffeine(rs.getInt("contains_caffeine"));
					foodItem.setContainsCaffeineUpdateDate(rs.getDate("contains_caffeine_update_date"));
					foodItem.setContainsSugarSubstitutes(rs.getInt("contains_sugar_substitutes"));
					foodItem.setContainsSugarSubstitutesUpdateDate(rs.getDate("contains_sugar_substitutes_update_date"));
					foodItem.setReferenceAmountG(rs.getDouble("reference_amount_g"));
					foodItem.setReferenceAmountMeasure(rs.getString("reference_amount_measure"));
					foodItem.setReferenceAmountUpdateDate(rs.getDate("reference_amount_update_date"));
					foodItem.setFoodGuideServingG(rs.getDouble("food_guide_serving_g"));
					foodItem.setFoodGuideServingMeasure(rs.getString("food_guide_serving_measure"));
					foodItem.setFoodGuideUpdateDate(rs.getDate("food_guide_update_date"));
					foodItem.setTier4ServingG(rs.getDouble("tier_4_serving_g"));
					foodItem.setTier4ServingMeasure(rs.getString("tier_4_serving_measure"));
					foodItem.setTier4ServingUpdateDate(rs.getDate("tier_4_serving_update_date"));
					foodItem.setRolledUp(rs.getInt("rolled_up"));
					foodItem.setRolledUpUpdateDate(rs.getDate("rolled_up_update_date"));
					foodItem.setApplySmallRaAdjustment(rs.getInt("apply_small_ra_adjustment"));
					foodItem.setComments(rs.getString("comments"));

					list.add(foodItem);
				}
			} catch(SQLException e) {
				e.printStackTrace();
			}

			// logger.error(new GsonBuilder().setDateFormat("yyyy-MM-dd").setPrettyPrinting().create().toJson(list));
		}

		return list;
	}
}
