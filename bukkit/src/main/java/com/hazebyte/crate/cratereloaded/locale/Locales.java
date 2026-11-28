package com.hazebyte.crate.cratereloaded.locale;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.hazebyte.crate.cratereloaded.CorePlugin;
import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Locales {

    private final CorePlugin plugin;
    private final LocaleManager localeManager;
    private final SetMultimap<String, Locale> loadedBundles = HashMultimap.create();
    private final List<String> bundles;
    private final String FOLDER = "messages";

    public Locales(CorePlugin plugin) {
        this.plugin = plugin;
        this.localeManager = new LocaleManager(getLocalization());
        this.bundles = findBundles();
        transferBundles();
    }

    public void loadLanguages() {
        Locale locale = getLocalization();
        for (String bundle : bundles) { // messages/core_zhs.properties
            String localeName = bundle.substring(bundle.indexOf("_") + 1, bundle.indexOf("."));
            if (locale.toString().equals(localeName)) {
                addMessageBundle(bundle, locale);
            }
        }
    }

    private List<String> findBundles() {
        List<String> bundles = new ArrayList<>();
        try {
            Path path = Paths.get(this.plugin.getFile().getAbsolutePath());
            FileSystem fileSystem =
                    FileSystems.newFileSystem(path, plugin.getClass().getClassLoader());
            Path filePath = fileSystem.getPath(File.separator + FOLDER + File.separator);

            Stream<Path> walk = Files.walk(filePath, 1);
            walk.forEach(file -> {
                String fileName = file.getFileName().toString();
                if (fileName.startsWith("core") && fileName.endsWith(".properties")) {
                    String name = file.toString();
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }
                    bundles.add(name);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to locate language bundles", e);
        }
        return bundles;
    }

    private void transferBundles() {
        File dataFolder = new File(plugin.getDataFolder(), FOLDER);
        dataFolder.mkdirs();
        String[] list = dataFolder.list();
        List<String> files = Arrays.asList(list);
        // Bundles are saved as messages/core_en.properties
        // Files are saved as core_en.properties
        for (String bundle : bundles) {
            // + 1 returns "core_en.properties" instead of "/core_en.properties"
            if (!files.contains(bundle.substring(bundle.lastIndexOf("/") + 1))) {
                plugin.saveResource(bundle, false);
            }
        }
    }

    private java.util.Properties bundled;

    /**
     * A message from the language files inside the jar. The copies in plugins/CrateReloaded/messages are never
     * overwritten, so after an update they lack keys added since: those showed as "Missing core.xyz" instead of
     * text. The configured language is tried first, then English.
     */
    private synchronized String bundledMessage(String key) {
        if (bundled == null) {
            java.util.Properties english = load(FOLDER + "/core_en.properties", null);
            String lang = this.plugin.getConfig().getString("locale", "en").toLowerCase(Locale.ROOT);
            bundled = load(FOLDER + "/core_" + lang + ".properties", english);
        }
        return bundled.getProperty(key);
    }

    private java.util.Properties load(String resource, java.util.Properties defaults) {
        java.util.Properties properties = new java.util.Properties(defaults);
        try (java.io.InputStream in = plugin.getResource(resource)) {
            if (in != null) {
                properties.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to read bundled " + resource, e);
        }
        return properties;
    }

    public Locale getDefaultLocale() {
        return this.localeManager.getDefaultLocale();
    }

    public void setDefaultLocale(Locale locale) {
        this.localeManager.setDefaultLocale(locale);
    }

    /**
     * https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
     *
     * @return
     */
    public Locale getLocalization() {
        String subtag = this.plugin.getConfig().getString("locale", "en").toLowerCase();
        return Locale.forLanguageTag(subtag);
    }

    public String getMessage(String key) {
        final MessageKey msgKey = MessageKey.of(key).getMessageKey();
        String message = this.localeManager.getMessage(msgKey);
        if (message == null) {
            message = bundledMessage(msgKey.getKey());
        }
        if (message == null) {
            message = String.format("Missing %s", msgKey.getKey());
        }
        return message;
    }

    public String getMessage(MessageKey key) {
        final MessageKey msgKey = key.getMessageKey();
        String message = this.localeManager.getMessage(msgKey);
        if (message == null) {
            message = bundledMessage(msgKey.getKey());
        }
        if (message == null) {
            message = String.format("Missing %s", msgKey.getKey());
        }
        return message;
    }

    public void addMessageBundle(String bundle, Locale locale) {
        if (!loadedBundles.containsEntry(bundle, locale)) {
            loadedBundles.put(bundle, locale);
            this.localeManager.addMessageBundle(
                    bundle, this.plugin.getDataFolder().getAbsolutePath(), locale);
        }
    }
}
