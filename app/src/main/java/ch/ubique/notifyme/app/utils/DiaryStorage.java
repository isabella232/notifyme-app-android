package ch.ubique.notifyme.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import org.crowdnotifier.android.sdk.model.DayDate;

import ch.ubique.notifyme.app.model.DiaryEntry;
import ch.ubique.notifyme.app.model.DiaryEntryDeprecatedV2;

public class DiaryStorage {

	private static final String KEY_DIARY_STORE = "KEY_DIARY_STORE";
	private static final String KEY_DIARY_ENTRIES = "KEY_DIARY_ENTRIES";
	private static final String KEY_DIARY_ENTRIES_V3 = "KEY_DIARY_ENTRIES_V3";
	private static final Type EXPOSURE_LIST_TYPE = new TypeToken<ArrayList<DiaryEntryDeprecatedV2>>() { }.getType();
	private static final Type EXPOSURE_LIST_V3_TYPE = new TypeToken<ArrayList<DiaryEntry>>() { }.getType();


	private static DiaryStorage instance;

	private SharedPreferences sharedPreferences;
	private Gson gson = new Gson();

	private DiaryStorage(Context context) {
		try {
			String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			sharedPreferences = EncryptedSharedPreferences.create(KEY_DIARY_STORE,
					KEY_ALIAS,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
		} catch (GeneralSecurityException | IOException ex) {
			ex.printStackTrace();
		}
	}

	public static synchronized DiaryStorage getInstance(Context context) {
		if (instance == null) {
			instance = new DiaryStorage(context);
		}
		return instance;
	}

	public boolean addEntry(DiaryEntry diaryEntry) {
		List<DiaryEntry> diaryEntries = getEntries();
		if (hasExposureWithId(diaryEntry.getId())) return false;
		diaryEntries.add(diaryEntry);
		saveToPrefs(diaryEntries);
		return true;
	}

	public boolean updateEntry(DiaryEntry newDiaryEntry) {
		List<DiaryEntry> diaryEntries = getEntries();
		DiaryEntry oldDiaryEntry = getDiaryEntryWithId(diaryEntries, newDiaryEntry.getId());
		if (oldDiaryEntry == null) return false;
		diaryEntries.remove(oldDiaryEntry);
		diaryEntries.add(newDiaryEntry);
		saveToPrefs(diaryEntries);
		return true;
	}

	public boolean removeEntry(long id) {
		List<DiaryEntry> diaryEntries = getEntries();
		DiaryEntry diaryEntry = getDiaryEntryWithId(diaryEntries, id);
		if (diaryEntry == null) return false;
		diaryEntries.remove(diaryEntry);
		saveToPrefs(diaryEntries);
		return true;
	}

	public boolean hasExposureWithId(long id) {
		return getDiaryEntryWithId(id) != null;
	}

	public DiaryEntry getDiaryEntryWithId(long id) {
		return getDiaryEntryWithId(getEntries(), id);
	}

	private DiaryEntry getDiaryEntryWithId(List<DiaryEntry> diaryEntries, long id) {
		for (DiaryEntry exposure : diaryEntries) {
			if (exposure.getId() == id) {
				return exposure;
			}
		}
		return null;
	}

	public List<DiaryEntry> getEntries() {
		migrateDiaryEntriesIfNecessary();
		return gson.fromJson(sharedPreferences.getString(KEY_DIARY_ENTRIES_V3, "[]"), EXPOSURE_LIST_V3_TYPE);
	}

	private void migrateDiaryEntriesIfNecessary() {
		if (!sharedPreferences.contains(KEY_DIARY_ENTRIES)) return;
		ArrayList<DiaryEntryDeprecatedV2> oldDiaryEntries =
				gson.fromJson(sharedPreferences.getString(KEY_DIARY_ENTRIES, "[]"), EXPOSURE_LIST_TYPE);
		ArrayList<DiaryEntry> diaryEntries = new ArrayList<>();
		for (DiaryEntryDeprecatedV2 oldDiaryEntry : oldDiaryEntries) {
			diaryEntries.add(oldDiaryEntry.toDiaryEntry());
		}
		saveToPrefs(diaryEntries);
		sharedPreferences.edit().remove(KEY_DIARY_ENTRIES).apply();
	}

	public void removeEntriesBefore(int maxDaysToKeep) {
		List<DiaryEntry> exposureList = getEntries();
		DayDate lastDateToKeep = new DayDate().subtractDays(maxDaysToKeep);
		Iterator<DiaryEntry> iterator = exposureList.iterator();
		while (iterator.hasNext()) {
			if (new DayDate(iterator.next().getDepartureTime()).isBefore(lastDateToKeep)) {
				iterator.remove();
			}
		}
		saveToPrefs(exposureList);
	}

	private void saveToPrefs(List<DiaryEntry> diaryEntries) {
		sharedPreferences.edit().putString(KEY_DIARY_ENTRIES_V3, gson.toJson(diaryEntries)).apply();
	}

	public void clear() {
		saveToPrefs(new ArrayList<>());
	}

}
