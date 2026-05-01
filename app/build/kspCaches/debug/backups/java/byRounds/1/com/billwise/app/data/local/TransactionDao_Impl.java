package com.billwise.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.billwise.app.domain.model.TransactionSource;
import com.billwise.app.domain.model.TransactionType;
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
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransactionEntity> __insertionAdapterOfTransactionEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteTransactionById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransactionEntity = new EntityInsertionAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`amount`,`merchant`,`datetime`,`type`,`category`,`source`,`isIgnored`,`merchantAlias`,`accountHint`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindDouble(2, entity.getAmount());
        statement.bindString(3, entity.getMerchant());
        statement.bindLong(4, entity.getDatetime());
        final String _tmp = __converters.fromTransactionType(entity.getType());
        statement.bindString(5, _tmp);
        statement.bindString(6, entity.getCategory());
        final String _tmp_1 = __converters.fromTransactionSource(entity.getSource());
        statement.bindString(7, _tmp_1);
        final int _tmp_2 = entity.isIgnored() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        if (entity.getMerchantAlias() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getMerchantAlias());
        }
        if (entity.getAccountHint() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getAccountHint());
        }
      }
    };
    this.__preparedStmtOfDeleteTransactionById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions";
        return _query;
      }
    };
  }

  @Override
  public Object insertTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionEntity.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTransactions(final List<TransactionEntity> transactions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionEntity.insert(transactions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTransactionById(final String id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTransactionById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteTransactionById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TransactionEntity>> getAllTransactions() {
    final String _sql = "SELECT * FROM transactions ORDER BY datetime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfDatetime = CursorUtil.getColumnIndexOrThrow(_cursor, "datetime");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfMerchantAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "merchantAlias");
          final int _cursorIndexOfAccountHint = CursorUtil.getColumnIndexOrThrow(_cursor, "accountHint");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpMerchant;
            _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            final long _tmpDatetime;
            _tmpDatetime = _cursor.getLong(_cursorIndexOfDatetime);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toTransactionType(_tmp);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final TransactionSource _tmpSource;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfSource);
            _tmpSource = __converters.toTransactionSource(_tmp_1);
            final boolean _tmpIsIgnored;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp_2 != 0;
            final String _tmpMerchantAlias;
            if (_cursor.isNull(_cursorIndexOfMerchantAlias)) {
              _tmpMerchantAlias = null;
            } else {
              _tmpMerchantAlias = _cursor.getString(_cursorIndexOfMerchantAlias);
            }
            final String _tmpAccountHint;
            if (_cursor.isNull(_cursorIndexOfAccountHint)) {
              _tmpAccountHint = null;
            } else {
              _tmpAccountHint = _cursor.getString(_cursorIndexOfAccountHint);
            }
            _item = new TransactionEntity(_tmpId,_tmpAmount,_tmpMerchant,_tmpDatetime,_tmpType,_tmpCategory,_tmpSource,_tmpIsIgnored,_tmpMerchantAlias,_tmpAccountHint);
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
  public Object getTransactionsInRange(final long startTime, final long endTime,
      final Continuation<? super List<TransactionEntity>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE datetime >= ? AND datetime <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfDatetime = CursorUtil.getColumnIndexOrThrow(_cursor, "datetime");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfMerchantAlias = CursorUtil.getColumnIndexOrThrow(_cursor, "merchantAlias");
          final int _cursorIndexOfAccountHint = CursorUtil.getColumnIndexOrThrow(_cursor, "accountHint");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpMerchant;
            _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            final long _tmpDatetime;
            _tmpDatetime = _cursor.getLong(_cursorIndexOfDatetime);
            final TransactionType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toTransactionType(_tmp);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final TransactionSource _tmpSource;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfSource);
            _tmpSource = __converters.toTransactionSource(_tmp_1);
            final boolean _tmpIsIgnored;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp_2 != 0;
            final String _tmpMerchantAlias;
            if (_cursor.isNull(_cursorIndexOfMerchantAlias)) {
              _tmpMerchantAlias = null;
            } else {
              _tmpMerchantAlias = _cursor.getString(_cursorIndexOfMerchantAlias);
            }
            final String _tmpAccountHint;
            if (_cursor.isNull(_cursorIndexOfAccountHint)) {
              _tmpAccountHint = null;
            } else {
              _tmpAccountHint = _cursor.getString(_cursorIndexOfAccountHint);
            }
            _item = new TransactionEntity(_tmpId,_tmpAmount,_tmpMerchant,_tmpDatetime,_tmpType,_tmpCategory,_tmpSource,_tmpIsIgnored,_tmpMerchantAlias,_tmpAccountHint);
            _result.add(_item);
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
