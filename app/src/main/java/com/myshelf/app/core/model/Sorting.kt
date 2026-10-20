package com.myshelf.app.core.model

enum class ItemSort(val label: String) {
    NEWEST("Recently added"), OLDEST("First added"),
    TITLE_ASC("Title: A–Z"), TITLE_DESC("Title: Z–A"),
    RATING_DESC("Highest rated"), RATING_ASC("Lowest rated"),
    YEAR_DESC("Year: newest"), YEAR_ASC("Year: oldest")
}

enum class CollectionSort(val label: String) {
    RECENT("Recent additions"), NAME_ASC("Name: A–Z"), NAME_DESC("Name: Z–A"),
    MOST_TITLES("Most titles"), FEWEST_TITLES("Fewest titles")
}
