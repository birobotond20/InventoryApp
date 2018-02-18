# InventoryApp
Project for Udacity, Android Basics Nanodegree Program by Google

The main idea of the project was that the app should help manage a fictional store's inventory. This is achieved by storing
the inventory items and their details (ID, item name, price, quantity, supplier and item image) in a database. To manage the
database the app uses a Contract class, a Helper class that extends from the SQLiteOpenHelper class, a provider class that
extends from the ContentProvider class and a CursorLoader.

The app consists of three main screens: a "Catalog", where the items are listed in a ListView, an "Add a Product" screen where
new items and their details can be added, and an "Edit Product" screen where the details of already existing products can be
modified.

There is also the possibility to add dummy data for testing purposes, delete all products, or delete just one. Within the app
the user can order more from one item from the given supplier via e-mail.
