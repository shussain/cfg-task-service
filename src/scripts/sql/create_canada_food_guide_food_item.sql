CREATE TABLE IF NOT EXISTS canada_food_guide_food_item (
	type                                   	 boolean,
	code                                   	 int4,
	name                                   	 text,
	cnf_group_code                         	 int4,
	cfg_code                               	 int4,
	cfg_code_update_date                   	 date,
	energy_kcal                            	 float8,
	sodium_amount_per_100g                 	 float8,
	sodium_imputation_reference            	 text,
	sodium_imputation_date                 	 date,
	sugar_amount_per_100g                  	 float8,
	sugar_imputation_reference             	 text,
	sugar_imputation_date                  	 date,
	transfat_amount_per_100g               	 float8,
	transfat_imputation_reference          	 text,
	transfat_imputation_date               	 date,
	satfat_amount_per_100g                 	 float8,
	satfat_imputation_reference            	 text,
	satfat_imputation_date                 	 date,
	totalfat_amount_per_100g               	 float8,
	totalfat_imputation_reference          	 text,
	totalfat_imputation_date               	 date,
	contains_added_sodium                  	 boolean,
	contains_added_sodium_update_date      	 date,
	contains_added_sugar                   	 boolean,
	contains_added_sugar_update_date       	 date,
	contains_free_sugars                   	 boolean,
	contains_free_sugars_update_date       	 date,
	contains_added_fat                     	 boolean,
	contains_added_fat_update_date         	 date,
	contains_added_transfat                	 boolean,
	contains_added_transfat_update_date    	 date,
	contains_caffeine                      	 boolean,
	contains_caffeine_update_date          	 date,
	contains_sugar_substitutes             	 boolean,
	contains_sugar_substitutes_update_date 	 date,
	reference_amount_g                     	 float8,
	reference_amount_measure               	 text,
	reference_amount_update_date           	 date,
	food_guide_serving_g                   	 float8,
	food_guide_serving_measure             	 text,
	food_guide_update_date                 	 date,
	tier_4_serving_g                       	 float8,
	tier_4_serving_measure                 	 text,
	tier_4_serving_update_date             	 date,
	rolled_up                              	 boolean,
	rolled_up_update_date                  	 date,
	override_small_ra_adjustment           	 boolean,
	toddler_item                           	 boolean,
	replacement_code                       	 int4,
	commit_date                            	 date,
	comments                               	 text,
	PRIMARY KEY (code)
);
