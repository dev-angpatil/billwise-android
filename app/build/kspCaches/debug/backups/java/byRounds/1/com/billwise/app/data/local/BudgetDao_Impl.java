package com.billwise.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BudgetDao_Impl implements BudgetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BudgetEntity> __insertionAdapterOfBudgetEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateNotified75;

  private final SharedSQLiteStatement __preparedStmtOfUpdateNotified100;

  public BudgetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBudgetEntity = new EntityInsertionAdapter<BudgetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `budgets` (`id`,`category`,`monthlyLimit`,`month`,`year`,`hasNotified75`,`hasNotified100`,`lastMonthSpend`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BudgetEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCategory());
        statement.bindDouble(3, entity.getMonthlyLimit());
        statement.bindLong(4, entity.getMonth());
        statement.bindLong(5, entity.getYear());
        final int _tmp = entity.getHasNotified75() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.getHasNotified100() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindDouble(8, entity.getLastMonthSpend());
      }
    };
    this.__preparedStmtOfUpdateNotified75 = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE budgets SET hasNotified75 = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateNotified100 = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE budgets SET hasNotified100 = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertBudget(final BudgetEntity budget,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBudgetEntity.insert(budget);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNotified75(final long id, final boolean notified,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateNotified75.acquire();
        int _argIndex = 1;
        final int _tmp = notified ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateNotified75.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNotified100(final long id, final boolean notified,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateNotified100.acquire();
        int _argIndex = 1;
        final int _tmp = notified ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateNotified100.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BudgetEntity>> getBudgetsForMonth(final int month, final int year) {
    final String _sql = "SELECT * FROM budgets WHERE month = ? AND year = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, month);
    _argIndex = 2;
    _statement.bindLong(_argIndex, year);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"budgets"}, new Callable<List<BudgetEntity>>() {
      @Override
      @NonNull
      public List<BudgetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfMonthlyLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyLimit");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfHasNotified75 = CursorUtil.getColumnIndexOrThrow(_cursor, "hasNotified75");
          final int _cursorIndexOfHasNotified100 = CursorUtil.getColumnIndexOrThrow(_cursor, "hasNotified100");
          final int _cursorIndexOfLastMonthSpend = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMonthSpend");
          final List<BudgetEntity> _result = new ArrayList<BudgetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BudgetEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpMonthlyLimit;
            _tmpMonthlyLimit = _cursor.getDouble(_cursorIndexOfMonthlyLimit);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final boolean _tmpHasNotified75;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasNotified75);
            _tmpHasNotified75 = _tmp != 0;
            final boolean _tmpHasNotified100;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasNotified100);
            _tmpHasNotified100 = _tmp_1 != 0;
            final double _tmpLastMonthSpend;
            _tmpLastMonthSpend = _cursor.getDouble(_cursorIndexOfLastMonthSpend);
            _item = new BudgetEntity(_tmpId,_tmpCategory,_tmpMonthlyLimit,_tmpMonth,_tmpYear,_tmpHasNotified75,_tmpHasNotified100,_tmpLastMonthSpend);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getBudgetByCategory(final String category, final int month, final int year,
      final Continuation<? super BudgetEntity> $completion) {
    final String _sql = "SELECT * FROM budgets WHERE category = ? AND month = ? AND year = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    _argIndex = 2;
    _statement.bindLong(_argIndex, month);
    _argIndex = 3;
    _statement.bindLong(_argIndex, year);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BudgetEntity>() {
      @Override
      @Nullable
      public BudgetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfMonthlyLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "monthlyLimit");
          final int _cursorIndexOfMonth = CursorUtil.getColumnIndexOrThrow(_cursor, "month");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfHasNotified75 = CursorUtil.getColumnIndexOrThrow(_cursor, "hasNotified75");
          final int _cursorIndexOfHasNotified100 = CursorUtil.getColumnIndexOrThrow(_cursor, "hasNotified100");
          final int _cursorIndexOfLastMonthSpend = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMonthSpend");
          final BudgetEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpMonthlyLimit;
            _tmpMonthlyLimit = _cursor.getDouble(_cursorIndexOfMonthlyLimit);
            final int _tmpMonth;
            _tmpMonth = _cursor.getInt(_cursorIndexOfMonth);
            final int _tmpYear;
            _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            final boolean _tmpHasNotified75;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasNotified75);
            _tmpHasNotified75 = _tmp != 0;
            final boolean _tmpHasNotified100;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasNotified100);
            _tmpHasNotified100 = _tmp_1 != 0;
            final double _tmpLastMonthSpend;
            _tmpLastMonthSpend = _cursor.getDouble(_cursorIndexOfLastMonthSpend);
            _result = new BudgetEntity(_tmpId,_tmpCategory,_tmpMonthlyLimit,_tmpMonth,_tmpYear,_tmpHasNotified75,_tmpHasNotified100,_tmpLastMonthSpend);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
