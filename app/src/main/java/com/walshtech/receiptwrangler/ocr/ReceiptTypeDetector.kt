/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.ocr

/**
 * Receipt type detection for specialized parsing
 */
enum class ReceiptType {
    RETAIL,           // Clothing, general shopping
    GROCERY,          // Food, household items
    RESTAURANT,       // Dining out, delivery
    FUEL,             // Gas station
    HOTEL,            // Hotel, lodging
    AIRLINE,          // Flight tickets
    AUTOMOTIVE,       // Car service, parts
    MEDICAL,          // Pharmacy, doctor, dental
    ENTERTAINMENT,    // Movie, concert, event
    UTILITY,          // Water, electric, phone
    SUBSCRIPTION,     // Software, streaming
    PROFESSIONAL,     // Legal, accounting, consulting
    UNKNOWN
}

/**
 * Enhanced OCR parsing with receipt type detection
 */
object ReceiptTypeDetector {
    
    fun detectType(text: String, merchant: String): ReceiptType {
        val lowerText = text.lowercase()
        val lowerMerchant = merchant.lowercase()
        
        // Grocery stores
        if (matchesPattern(lowerMerchant, GROCERY_KEYWORDS) || matchesPattern(lowerText, GROCERY_KEYWORDS)) {
            return ReceiptType.GROCERY
        }
        
        // Restaurants
        if (matchesPattern(lowerMerchant, RESTAURANT_KEYWORDS) || matchesPattern(lowerText, RESTAURANT_KEYWORDS)) {
            return ReceiptType.RESTAURANT
        }
        
        // Gas/Fuel
        if (matchesPattern(lowerMerchant, FUEL_KEYWORDS) || matchesPattern(lowerText, FUEL_KEYWORDS)) {
            return ReceiptType.FUEL
        }
        
        // Hotels
        if (matchesPattern(lowerMerchant, HOTEL_KEYWORDS) || matchesPattern(lowerText, HOTEL_KEYWORDS)) {
            return ReceiptType.HOTEL
        }
        
        // Airlines
        if (matchesPattern(lowerMerchant, AIRLINE_KEYWORDS) || matchesPattern(lowerText, AIRLINE_KEYWORDS)) {
            return ReceiptType.AIRLINE
        }
        
        // Automotive
        if (matchesPattern(lowerMerchant, AUTOMOTIVE_KEYWORDS) || matchesPattern(lowerText, AUTOMOTIVE_KEYWORDS)) {
            return ReceiptType.AUTOMOTIVE
        }
        
        // Medical/Pharmacy
        if (matchesPattern(lowerMerchant, MEDICAL_KEYWORDS) || matchesPattern(lowerText, MEDICAL_KEYWORDS)) {
            return ReceiptType.MEDICAL
        }
        
        // Entertainment
        if (matchesPattern(lowerMerchant, ENTERTAINMENT_KEYWORDS) || matchesPattern(lowerText, ENTERTAINMENT_KEYWORDS)) {
            return ReceiptType.ENTERTAINMENT
        }
        
        // Utilities
        if (matchesPattern(lowerMerchant, UTILITY_KEYWORDS) || matchesPattern(lowerText, UTILITY_KEYWORDS)) {
            return ReceiptType.UTILITY
        }
        
        // Subscriptions
        if (matchesPattern(lowerMerchant, SUBSCRIPTION_KEYWORDS) || matchesPattern(lowerText, SUBSCRIPTION_KEYWORDS)) {
            return ReceiptType.SUBSCRIPTION
        }
        
        // Professional Services
        if (matchesPattern(lowerMerchant, PROFESSIONAL_KEYWORDS) || matchesPattern(lowerText, PROFESSIONAL_KEYWORDS)) {
            return ReceiptType.PROFESSIONAL
        }
        
        // Retail
        if (matchesPattern(lowerMerchant, RETAIL_KEYWORDS) || matchesPattern(lowerText, RETAIL_KEYWORDS)) {
            return ReceiptType.RETAIL
        }
        
        return ReceiptType.UNKNOWN
    }
    
    private fun matchesPattern(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
    
    private val GROCERY_KEYWORDS = listOf(
        "supermarket", "grocery", "whole foods", "trader joe", "safeway", "kroger", "trader joe's",
        "whole foods market", "publix", "albertsons", "costco", "walmart", "target",
        "food lion", "harris teeter", "instacart", "fresh direct", "shipt"
    )
    
    private val RESTAURANT_KEYWORDS = listOf(
        "restaurant", "cafe", "coffee", "pizza", "burger", "bbq", "sushi", "cafe",
        "diner", "bistro", "tavern", "bar", "grill", "steakhouse", "doordash", "ubereats",
        "grubhub", "delivery", "order pickup", "chipotle", "taco bell", "mcdonalds",
        "starbucks", "dunkin", "applebee", "olive garden"
    )
    
    private val FUEL_KEYWORDS = listOf(
        "gas station", "fuel", "shell", "exxon", "chevron", "bp", "mobil", "sunoco",
        "speedway", "circle k", "texaco", "gulf", "murphy", "pilot", "loves"
    )
    
    private val HOTEL_KEYWORDS = listOf(
        "hotel", "motel", "inn", "resort", "lodge", "hilton", "marriott", "hyatt",
        "sheraton", "holiday inn", "ramada", "best western", "airbnb", "booking", "expedia"
    )
    
    private val AIRLINE_KEYWORDS = listOf(
        "airline", "flight", "delta", "united", "american airlines", "southwest", "jetblue",
        "alaska airlines", "spirit", "frontier", "lufthansa", "british airways", "airline ticket"
    )
    
    private val AUTOMOTIVE_KEYWORDS = listOf(
        "auto", "car", "vehicle", "maintenance", "repair", "oil change", "tire", "brake",
        "dealer", "mechanic", "gas", "petrol", "pump", "ford", "chevy", "tesla"
    )
    
    private val MEDICAL_KEYWORDS = listOf(
        "pharmacy", "doctor", "hospital", "clinic", "dental", "vision", "prescription",
        "cvs", "walgreens", "rite aid", "medical", "health", "medicine", "pill", "drug"
    )
    
    private val ENTERTAINMENT_KEYWORDS = listOf(
        "movie", "cinema", "theater", "concert", "ticket", "event", "amusement", "park",
        "zoo", "museum", "gallery", "show", "gaming", "arcade", "bowling"
    )
    
    private val UTILITY_KEYWORDS = listOf(
        "electric", "water", "gas", "phone", "internet", "utility", "power", "telecom",
        "bill", "comcast", "verizon", "at&t", "spectrum"
    )
    
    private val SUBSCRIPTION_KEYWORDS = listOf(
        "subscription", "netflix", "hulu", "disney", "spotify", "adobe", "microsoft",
        "subscription", "monthly", "annual", "premium", "membership", "saas"
    )
    
    private val PROFESSIONAL_KEYWORDS = listOf(
        "attorney", "lawyer", "accountant", "cpa", "consulting", "audit", "legal",
        "professional services", "office", "corporate"
    )
    
    private val RETAIL_KEYWORDS = listOf(
        "store", "retail", "apparel", "clothing", "shoe", "department", "mall",
        "nordstrom", "macy", "gap", "banana republic", "old navy", "h&m", "forever 21"
    )
}

/**
 * Type-specific parsing enhancements
 */
object TypeSpecificParser {
    
    fun enhanceForType(text: String, type: ReceiptType): Map<String, String> {
        return when (type) {
            ReceiptType.RESTAURANT -> parseRestaurant(text)
            ReceiptType.FUEL -> parseFuel(text)
            ReceiptType.AIRLINE -> parseAirline(text)
            ReceiptType.HOTEL -> parseHotel(text)
            ReceiptType.GROCERY -> parseGrocery(text)
            else -> emptyMap()
        }
    }
    
    private fun parseRestaurant(text: String): Map<String, String> {
        val extras = mutableMapOf<String, String>()
        
        // Extract tip
        if (text.contains("tip", ignoreCase = true)) {
            val tipAmount = extractAmount(text, "tip")
            if (tipAmount.isNotEmpty()) extras["tip"] = tipAmount
        }
        
        // Extract service charge
        if (text.contains("service charge", ignoreCase = true)) {
            val serviceAmount = extractAmount(text, "service")
            if (serviceAmount.isNotEmpty()) extras["service_charge"] = serviceAmount
        }
        
        // Extract cover count
        val coverMatch = Regex("""(\d+)\s+cover""", RegexOption.IGNORE_CASE).find(text)
        if (coverMatch != null) {
            extras["covers"] = coverMatch.groupValues[1]
        }
        
        return extras
    }
    
    private fun parseFuel(text: String): Map<String, String> {
        val extras = mutableMapOf<String, String>()
        
        // Extract gallons
        val gallonMatch = Regex("""(\d+\.?\d*)\s*gal""", RegexOption.IGNORE_CASE).find(text)
        if (gallonMatch != null) {
            extras["gallons"] = gallonMatch.groupValues[1]
        }
        
        // Extract per-gallon price
        val priceMatch = Regex("""(?:per\s+gal|$)\s*(\d+\.\d{2})""", RegexOption.IGNORE_CASE).find(text)
        if (priceMatch != null) {
            extras["price_per_gallon"] = priceMatch.groupValues[1]
        }
        
        // Extract fuel grade
        if (text.contains("premium", ignoreCase = true)) extras["grade"] = "premium"
        else if (text.contains("regular", ignoreCase = true)) extras["grade"] = "regular"
        else if (text.contains("diesel", ignoreCase = true)) extras["grade"] = "diesel"
        
        return extras
    }
    
    private fun parseAirline(text: String): Map<String, String> {
        val extras = mutableMapOf<String, String>()
        
        // Extract confirmation number
        val confirmMatch = Regex("""(?:confirmation|ref|pnr)[:\s]*([A-Z0-9]{6})""", RegexOption.IGNORE_CASE).find(text)
        if (confirmMatch != null) {
            extras["confirmation"] = confirmMatch.groupValues[1]
        }
        
        // Extract flight number
        val flightMatch = Regex("""(?:flight|flight)[:\s]*([A-Z]+\s*\d+)""", RegexOption.IGNORE_CASE).find(text)
        if (flightMatch != null) {
            extras["flight"] = flightMatch.groupValues[1]
        }
        
        return extras
    }
    
    private fun parseHotel(text: String): Map<String, String> {
        val extras = mutableMapOf<String, String>()
        
        // Extract night count
        val nightMatch = Regex("""(\d+)\s*(?:night|nite)""", RegexOption.IGNORE_CASE).find(text)
        if (nightMatch != null) {
            extras["nights"] = nightMatch.groupValues[1]
        }
        
        // Extract room rate
        val roomMatch = Regex("""room[:\s]*\$?([\d,.]+)""", RegexOption.IGNORE_CASE).find(text)
        if (roomMatch != null) {
            extras["room_rate"] = roomMatch.groupValues[1]
        }
        
        return extras
    }
    
    private fun parseGrocery(text: String): Map<String, String> {
        val extras = mutableMapOf<String, String>()
        
        // Count items
        val itemMatch = Regex("""(\d+)\s*item""", RegexOption.IGNORE_CASE).find(text)
        if (itemMatch != null) {
            extras["items"] = itemMatch.groupValues[1]
        }
        
        return extras
    }
    
    private fun extractAmount(text: String, keyword: String): String {
        val pattern = Regex("""$keyword[:\s]*\$?([\d,.]+)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: ""
    }
}
