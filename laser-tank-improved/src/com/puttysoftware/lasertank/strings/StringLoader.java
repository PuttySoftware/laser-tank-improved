package com.puttysoftware.lasertank.strings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import com.puttysoftware.dialogs.CommonDialogs;
import com.puttysoftware.fileio.ResourceStreamReader;
import com.puttysoftware.lasertank.LaserTank;
import com.puttysoftware.lasertank.prefs.PreferencesManager;
import com.puttysoftware.lasertank.resourcemanagers.ImageManager;
import com.puttysoftware.lasertank.stringmanagers.StringConstants;
import com.puttysoftware.lasertank.strings.global.GlobalLoader;
import com.puttysoftware.lasertank.utilities.ArenaConstants;
import com.puttysoftware.lasertank.utilities.DifficultyConstants;
import com.puttysoftware.lasertank.utilities.Extension;

public class StringLoader {
    private static final String LOCALIZED_LANGUAGES_FILE_NAME = "localizedlanguages.txt";
    private static final String LOAD_PATH = "/assets/locale/";
    private static Class<?> LOAD_CLASS = StringLoader.class;
    private static ArrayList<Properties> CACHE;
    private static ArrayList<String> LOCALIZED_LANGUAGES_CACHE;
    private static int LANGUAGE_ID = 0;
    private static String LANGUAGE_NAME = null;

    public static void activeLanguageChanged(final int newLanguageID) {
	StringLoader.CACHE = null;
	StringLoader.LOCALIZED_LANGUAGES_CACHE = null;
	StringLoader.LANGUAGE_ID = newLanguageID;
	StringLoader.LANGUAGE_NAME = GlobalLoader.loadLanguage(StringLoader.LANGUAGE_ID) + "/";
	DifficultyConstants.reloadDifficultyNames();
	ArenaConstants.activeLanguageChanged();
	LaserTank.getApplication().activeLanguageChanged();
	PreferencesManager.activeLanguageChanged();
	ImageManager.activeLanguageChanged();
    }

    public static String getLanguageName() {
	return StringLoader.LANGUAGE_NAME;
    }

    public static String getLocalizedLanguage(final int languageID) {
	StringLoader.loadLocalizedLanguagesList();
	return StringLoader.LOCALIZED_LANGUAGES_CACHE.get(languageID);
    }

    private static void loadLocalizedLanguagesList() {
	if (StringLoader.LOCALIZED_LANGUAGES_CACHE == null) {
	    StringLoader.LOCALIZED_LANGUAGES_CACHE = new ArrayList<>();
	    final String filename = StringLoader.LOCALIZED_LANGUAGES_FILE_NAME;
	    try (final InputStream is = StringLoader.LOAD_CLASS
		    .getResourceAsStream(StringLoader.LOAD_PATH + StringLoader.LANGUAGE_NAME + filename);
		    final ResourceStreamReader rsr = new ResourceStreamReader(is, "UTF-8")) {
		String line = StringConstants.COMMON_STRING_EMPTY;
		while (line != null) {
		    // Read line
		    line = rsr.readString();
		    if (line != null) {
			// Parse line
			StringLoader.LOCALIZED_LANGUAGES_CACHE.add(line);
		    }
		}
	    } catch (final IOException ioe) {
		CommonDialogs.showErrorDialog(
			"Something has gone horribly wrong trying to load the local language data!", "FATAL ERROR");
		LaserTank.logErrorDirectly(ioe);
	    }
	}
    }

    public static void setDefaultLanguage() {
	GlobalLoader.initialize();
	StringLoader.LOCALIZED_LANGUAGES_CACHE = null;
	StringLoader.LANGUAGE_ID = 0;
	StringLoader.LANGUAGE_NAME = GlobalLoader.loadLanguage(StringLoader.LANGUAGE_ID) + "/";
	final int files = StringFileNames.getFileCount();
	StringLoader.CACHE = new ArrayList<>(files);
	for (int f = 0; f < files; f++) {
	    StringLoader.CACHE.add(new Properties());
	}
	StringLoader.cacheFile(StringFile.DIFFICULTY);
	StringLoader.cacheFile(StringFile.ERRORS);
	StringLoader.cacheFile(StringFile.PREFS);
	StringLoader.cacheFile(StringFile.GENERIC);
	StringLoader.cacheFile(StringFile.OBJECTS);
	StringLoader.cacheFile(StringFile.MENUS);
	StringLoader.cacheFile(StringFile.DIALOGS);
	StringLoader.cacheFile(StringFile.MESSAGES);
	StringLoader.cacheFile(StringFile.EDITOR);
	StringLoader.cacheFile(StringFile.GAME);
	StringLoader.cacheFile(StringFile.TIME);
    }

    private static void cacheFile(final StringFile file) {
	final int fileID = file.ordinal();
	final String filename = StringFileNames.getFileName(file);
	try (final InputStream is = StringLoader.LOAD_CLASS
		.getResourceAsStream(StringLoader.LOAD_PATH + filename + Extension.getStringsExtensionWithPeriod())) {
	    StringLoader.CACHE.get(fileID).load(is);
	} catch (final IOException ioe) {
	    CommonDialogs.showErrorDialog("Something has gone horribly wrong trying to cache global string data!",
		    "FATAL ERROR");
	    LaserTank.logErrorDirectly(ioe);
	}
    }

    private static Properties getFromCache(final StringFile file) {
	final int fileID = file.ordinal();
	return StringLoader.CACHE.get(fileID);
    }

    public static String loadCommon(final CommonString str) {
	return str.getValue();
    }

    public static String loadDifficulty(final DifficultyString str) {
	return StringLoader.getFromCache(StringFile.DIFFICULTY).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadEditor(final EditorString str) {
	return StringLoader.getFromCache(StringFile.EDITOR).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadError(final ErrorString str) {
	return StringLoader.getFromCache(StringFile.ERRORS).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadGame(final GameString str) {
	return StringLoader.getFromCache(StringFile.GAME).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadGeneric(final GenericString str) {
	return StringLoader.getFromCache(StringFile.GENERIC).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadMenu(final MenuString str) {
	return StringLoader.getFromCache(StringFile.MENUS).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadMessage(final MessageString str) {
	return StringLoader.getFromCache(StringFile.MESSAGES).getProperty(Integer.toString(str.ordinal()));
    }

    public static String loadPref(final PrefString str) {
	return StringLoader.getFromCache(StringFile.PREFS).getProperty(Integer.toString(str.ordinal()));
    }
}
