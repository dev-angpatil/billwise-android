package com.billwise.app

import android.app.Application

class BillWiseApplication : Application() {
    lateinit var db: com.billwise.app.data.local.AppDatabase
        private set
    lateinit var transactionRepository: com.billwise.app.data.repository.TransactionRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        db = androidx.room.Room.databaseBuilder(applicationContext, com.billwise.app.data.local.AppDatabase::class.java, "billwise-db")
            .addMigrations(com.billwise.app.data.local.AppDatabase.MIGRATION_1_2, com.billwise.app.data.local.AppDatabase.MIGRATION_2_3, com.billwise.app.data.local.AppDatabase.MIGRATION_3_4)
            .build()
        transactionRepository = com.billwise.app.data.repository.TransactionRepositoryImpl(db.transactionDao())
    }
}
