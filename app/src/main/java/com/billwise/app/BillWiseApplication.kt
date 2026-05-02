package com.billwise.app

import android.app.Application
import androidx.room.Room
import com.billwise.app.data.local.AppDatabase
import com.billwise.app.data.repository.TransactionRepositoryImpl
import com.billwise.app.data.parser.PdfParser

class BillWiseApplication : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var transactionRepository: TransactionRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "billwise-db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .fallbackToDestructiveMigration() // Safety fallback for any version mismatch
            .build()
        
        transactionRepository = TransactionRepositoryImpl(db.transactionDao())
        
        PdfParser.init(this)
    }
}
