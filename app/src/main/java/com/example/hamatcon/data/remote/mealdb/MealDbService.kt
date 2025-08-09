package com.example.hamatcon.data.remote.mealdb

import retrofit2.http.GET
import retrofit2.http.Query

interface MealDbService {
    // Categories: "Beef", "Chicken", "Vegetarian", "Dessert", etc.
    // Returns minimal info (id, name, thumb)
    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): FilterResponse

    // Full details for one meal by id
    @GET("lookup.php")
    suspend fun lookupById(@Query("i") idMeal: String): LookupResponse
}
