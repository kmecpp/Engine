package com.builtbroken.mc.lib.json;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.core.registry.implement.IPostInit;
import com.builtbroken.mc.core.registry.implement.IRecipeContainer;
import com.builtbroken.mc.core.registry.implement.IRegistryInit;
import com.builtbroken.mc.lib.json.imp.IJsonBlockSubProcessor;
import com.builtbroken.mc.lib.json.imp.IJsonGenObject;
import com.builtbroken.mc.lib.json.processors.JsonProcessor;
import com.builtbroken.mc.lib.json.processors.block.processor.JsonBlockProcessor;
import com.builtbroken.mc.lib.json.processors.extra.JsonOreNameProcessor;
import com.builtbroken.mc.lib.json.processors.item.JsonItemProcessor;
import com.builtbroken.mc.lib.json.processors.recipe.crafting.JsonCraftingRecipeProcessor;
import com.builtbroken.mc.lib.json.processors.recipe.smelting.JsonFurnaceRecipeProcessor;
import com.builtbroken.mc.lib.json.processors.world.JsonWorldOreGenProcessor;
import com.builtbroken.mc.lib.mod.loadable.AbstractLoadable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public final class JsonContentLoader extends AbstractLoadable
{
    /** Processor instance */
    public static JsonContentLoader INSTANCE = new JsonContentLoader();

    /** Internal files for loading */
    public final List<String> classPathResources = new ArrayList();
    /** External files for loading */
    public final List<File> externalFiles = new ArrayList();
    /** External jar files for loading */
    public final List<File> externalJarFiles = new ArrayList();

    /** Extensions that can be loaded by the system, defaults with .json */
    public final List<String> extensionsToLoad = new ArrayList();

    /** List of processors to handle files */
    public final HashMap<String, JsonProcessor> processors = new HashMap();
    /** Entries loaded from file system */
    public final HashMap<String, List<JsonEntry>> jsonEntries = new HashMap();
    /** List of objects generated by processors */
    public final List<IJsonGenObject> generatedObjects = new ArrayList();

    /** Block processor */
    public final JsonBlockProcessor blockProcessor;
    /** Item processor */
    public final JsonItemProcessor itemProcessor;
    /** Crafting grid recipe processor {@link net.minecraft.item.crafting.CraftingManager} */
    public final JsonCraftingRecipeProcessor craftingRecipeProcessor;
    /** Furnace recipe processor {@link net.minecraft.item.crafting.FurnaceRecipes} */
    public final JsonFurnaceRecipeProcessor furnaceRecipeProcessor;
    /** Processor for handling ore generation in the world */
    public final JsonWorldOreGenProcessor worldOreGenProcessor;


    /** Used almost entirely by unit testing to disable file loading */
    public boolean ignoreFileLoading = false;


    public File externalContentFolder;

    public JsonContentLoader()
    {
        extensionsToLoad.add("json");
        blockProcessor = new JsonBlockProcessor();
        itemProcessor = new JsonItemProcessor();
        craftingRecipeProcessor = new JsonCraftingRecipeProcessor();
        furnaceRecipeProcessor = new JsonFurnaceRecipeProcessor();
        worldOreGenProcessor = new JsonWorldOreGenProcessor();
    }

    /**
     * Adds the processor to the list of processors
     *
     * @param processor
     */
    public void add(JsonProcessor processor)
    {
        processors.put(processor.getJsonKey(), processor);
        if (processor instanceof IJsonBlockSubProcessor)
        {
            blockProcessor.addSubProcessor(processor.getJsonKey(), (IJsonBlockSubProcessor) processor);
        }
        //TODO add item sub processors
    }

    @Override
    public void preInit()
    {
        //Init data
        externalContentFolder = new File(References.BBM_CONFIG_FOLDER, "json");
        //Validate data
        validateFilePaths();
        //Load processors
        add(blockProcessor);
        add(itemProcessor);
        add(new JsonOreNameProcessor());
        //TODO add entity loading
        add(worldOreGenProcessor);
        add(craftingRecipeProcessor);
        add(furnaceRecipeProcessor);
        //TODO add machine recipes

        //Resources are loaded before they can be processed to allow early processing
        if (!ignoreFileLoading)
        {
            //Load resources from file system
            loadResources();
        }
        processEntries();
    }

    @Override
    public void init()
    {
        processEntries();
    }

    /**
     * Called to processe a single set of content
     * <p>
     * This is normally called outside of this class
     * when content needs to load before a set time
     * in order for minecraft to handle the content
     * correctly. Examples of this are item/block
     * textures that load before init() when most
     * content is actually loaded.
     *
     * @param processorKey
     */
    public void process(String processorKey)
    {
        if (jsonEntries.containsKey(processorKey))
        {
            final List<JsonEntry> entries = jsonEntries.get(processorKey);
            if (entries != null)
            {
                final Iterator<JsonEntry> it = entries.iterator();
                //Process all loaded elements
                while (it.hasNext())
                {
                    final JsonEntry entry = it.next();
                    try
                    {
                        if (process(entry.jsonKey, entry.element))
                        {
                            it.remove();
                        }
                    }
                    catch (Exception e)
                    {
                        //TODO figure out who made the file
                        //Crash as the file may be important
                        throw new RuntimeException("Failed to process entry from file " + entry.fileReadFrom + ". Make corrections to the file or contact the file's creator for the issue to be fixed.\n  Entry = " + entry, e);
                    }
                }
            }
            if (entries.size() <= 0)
            {
                jsonEntries.remove(processorKey);
            }
            else
            {
                jsonEntries.put(processorKey, entries);
            }
        }
    }

    /**
     * Called to process current entries that are loaded.
     * Only entries that have processors will be loaded
     * and then removed from the list of entries.
     */
    protected void processEntries()
    {
        if (jsonEntries.size() > 0)
        {
            //Collect all entries to sort
            final ArrayList<String> processorKeys = new ArrayList();
            for (final JsonProcessor processor : processors.values())
            {
                String jsonKey = processor.getJsonKey();
                String loadOrder = processor.getLoadOrder();
                if (loadOrder != null && !loadOrder.isEmpty())
                {
                    jsonKey += "@" + loadOrder;
                }
                processorKeys.add(jsonKey);
            }

            //Sort entries
            final List<String> sortingProcessorList = sortSortingValues(processorKeys);

            //Loop threw processors in order
            for (final String processorKey : sortingProcessorList)
            {
                process(processorKey);
            }
        }
    }

    /** Validates file paths and makes folders as needed */
    public void validateFilePaths()
    {
        if (!externalContentFolder.exists())
        {
            externalContentFolder.mkdirs();
        }
    }

    /** Loads resources from folders and class path */
    public void loadResources()
    {
        //TODO implement threading to allow other mods to load while we load content
        loadResourcesFromFolder(externalContentFolder);
        for (ModContainer container : Loader.instance().getModList())
        {
            //TODO load additional mod data directly from mod folder
            File file = container.getSource();
            System.out.println("" + file);
            Object mod = container.getMod();
            if (mod != null)
            {
                loadResourcesFromPackage(mod.getClass(), "content/" + container.getModId() + "/");
            }
        }

        //Load external files
        for (File file : externalFiles)
        {
            try
            {
                loadJsonFile(file, jsonEntries);
            }
            catch (IOException e)
            {
                //Crash as the file may be important
                throw new RuntimeException("Failed to load resource " + file, e);
            }
        }

        //Load internal files
        for (String resource : classPathResources)
        {
            try
            {
                loadJsonFileFromResources(resource, jsonEntries);
            }
            catch (IOException e)
            {
                //Crash as the file may be important
                throw new RuntimeException("Failed to load resource " + resource, e);
            }
        }
    }

    /**
     * Creates a map of entries used for sorting loaded files later
     *
     * @param sortingValues - values to sort, entries in list are consumed
     * @return Map of keys to sorting index values
     */
    public static List<String> sortSortingValues(List<String> sortingValues)
    {
        //Run a basic sorter on the list to order it values, after:value, before:value:
        Collections.sort(sortingValues, new StringSortingComparator());


        final LinkedList<String> sortedValues = new LinkedList();
        while (!sortingValues.isEmpty())
        {
            //Sort out list
            sortSortingValues(sortingValues, sortedValues);

            //Exit point, prevents inf loop by removing bad entries and adding them to the end of the sorting list
            if (!sortingValues.isEmpty())
            {
                //Loop threw what entries we have left
                final Iterator<String> it = sortingValues.iterator();
                while (it.hasNext())
                {
                    final String entry = it.next();
                    if (entry.contains("@"))
                    {
                        String[] split = entry.split("@");
                        final String name = entry.split("@")[0];

                        if (split[1].contains(":"))
                        {
                            split = split[1].split(":");
                            boolean found = false;

                            //Try too see if we have a valid entry left in our sorting list that might just contain a after: or before: preventing it from adding
                            for (final String v : sortingValues)
                            {
                                if (!v.equals(entry) && v.contains(split[1]))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            for (final String v : sortedValues)
                            {
                                if (!v.equals(entry) && v.contains(split[1]))
                                {
                                    found = true;
                                    break;
                                }
                            }

                            //If we have no category for the sorting entry add it to the master list
                            if (!found)
                            {
                                Engine.logger().error("Bad sorting value for " + entry + " could not find category for " + split[1]);
                                sortedValues.add(name);
                                it.remove();
                            }
                        }
                        //If entry is invalid add it
                        else
                        {
                            Engine.logger().error("Bad sorting value for " + entry + " has no valid sorting data");
                            sortedValues.add(name);
                            it.remove();
                        }
                    }
                    //Should never happen as entries with no sorting value should be added before here
                    else
                    {
                        sortedValues.add(entry);
                        it.remove();
                    }
                }
            }
        }
        return sortedValues;
    }

    /**
     * Sorts the string values that will later be used as an index value
     * to sort all .json files being processed
     *
     * @param sortingValues - list of unsorted values
     * @param sortedValues  - list of sorted values and where values will be inserted into
     */
    public static void sortSortingValues(List<String> sortingValues, List<String> sortedValues)
    {
        Iterator<String> it = sortingValues.iterator();
        while (it.hasNext())
        {
            String entry = it.next();
            if (entry.contains("@"))
            {
                String[] split = entry.split("@");
                String name = split[0];
                String sortValue = split[1];
                //TODO add support for ; allowing sorting of several values

                if (sortValue.contains(":"))
                {
                    split = sortValue.split(":");
                    String prefix = split[0];
                    String cat = split[1];
                    boolean catFound = false;

                    ListIterator<String> sortedIt = sortedValues.listIterator();
                    while (sortedIt.hasNext())
                    {
                        String v = sortedIt.next();
                        if (v.equalsIgnoreCase(cat))
                        {
                            catFound = true;
                            if (prefix.equalsIgnoreCase("after"))
                            {
                                sortedIt.add(name);
                            }
                            else if (prefix.equalsIgnoreCase("before"))
                            {
                                sortedIt.previous();
                                sortedIt.add(name);
                            }
                            else
                            {
                                Engine.logger().error("Bad sorting value for " + entry + " we can only read 'after' and 'before'");
                                sortedValues.add(name);
                                it.remove();
                            }
                            break;
                        }
                    }
                    if (catFound)
                    {
                        it.remove();
                    }
                }
                else
                {
                    sortedValues.add(name);
                    it.remove();
                }
            }
            else
            {
                sortedValues.add(entry);
                it.remove();
            }
        }
    }

    /**
     * Called to process a json element entry into a
     * generated object
     *
     * @param key     - json processor key
     * @param element - data
     * @return true if it was processed and added to the generated object list
     */
    public boolean process(String key, JsonElement element)
    {
        final JsonProcessor processor = processors.get(key);
        if (processor != null)
        {
            if (processor.canProcess(key, element))
            {
                if (processor.shouldLoad(element))
                {
                    List<IJsonGenObject> objects = new ArrayList();
                    processor.process(element, objects);

                    boolean reg = false;
                    for (IJsonGenObject data : objects)
                    {
                        if (data != null)
                        {
                            generatedObjects.add(data);
                            data.register();
                            if (data instanceof IRegistryInit)
                            {
                                ((IRegistryInit) data).onRegistered();
                            }
                            reg = true;
                        }
                    }
                    return reg;
                }
                return true; //Technically it was processed but just not added
            }
            else
            {
                //TODO add error handling
            }
        }
        return false;
    }

    /**
     * Called to load json files from the folder
     *
     * @param folder
     */
    public void loadResourcesFromFolder(File folder)
    {
        for (File file : folder.listFiles())
        {
            if (file.isDirectory())
            {
                loadResourcesFromFolder(folder);
            }
            else
            {
                String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length());
                if (extension.equalsIgnoreCase("jar"))
                {
                    externalJarFiles.add(file);
                }
                else if (extensionsToLoad.contains(extension))
                {
                    externalFiles.add(file);
                }
            }
        }
    }

    /**
     * Loads package
     *
     * @param folder - package your looking to load data from
     */
    public void loadResourcesFromPackage(Class clazz, String folder)
    {
        //http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
        try
        {

            InputStream stream = clazz.getClassLoader().getResourceAsStream(folder);
            if (stream != null)
            {
                final List<String> files = IOUtils.readLines(stream, Charsets.UTF_8);
                for (String name : files)
                {
                    final String path = folder + (!folder.endsWith("/") ? "/" : "") + name;
                    if (name.lastIndexOf(".") > 1)
                    {
                        String extension = name.substring(name.lastIndexOf(".") + 1, name.length());
                        if (extensionsToLoad.contains(extension))
                        {
                            classPathResources.add(path);
                        }
                    }
                    else
                    {
                        loadResourcesFromPackage(clazz, path + "/");
                    }
                }
            }
        }
        catch (Exception e)
        {
            Engine.logger().error("Failed to load resources from class path.", e);
        }
    }

    /**
     * Loads a json file from the resource path
     *
     * @param resource - resource location
     * @return json file as a json element object
     * @throws IOException
     */
    public static void loadJsonFileFromResources(String resource, HashMap<String, List<JsonEntry>> entries) throws IOException
    {
        if (resource != null && !resource.isEmpty())
        {
            URL url = JsonContentLoader.class.getClassLoader().getResource(resource);
            if (url != null)
            {
                InputStream stream = url.openStream();
                loadJson(resource, new InputStreamReader(stream), entries);
                stream.close();
            }
        }
    }

    /**
     * Loads a json file from the resource path
     *
     * @param file - file to read from
     * @return json file as a json element object
     * @throws IOException
     */
    public static void loadJsonFile(File file, HashMap<String, List<JsonEntry>> entries) throws IOException
    {
        if (file.exists() && file.isFile())
        {
            FileReader stream = new FileReader(file);
            loadJson(file.getName(), new BufferedReader(stream), entries);
            stream.close();
        }
    }

    /**
     * Loads a json file from a reader
     *
     * @param fileName - file the reader loaded from, used only for error logs
     * @param reader   - reader with the data
     * @param entries  - place to put json entries into
     */
    public static void loadJson(String fileName, Reader reader, HashMap<String, List<JsonEntry>> entries)
    {
        JsonReader jsonReader = new JsonReader(reader);
        JsonElement element = Streams.parse(jsonReader);
        loadJsonElement(fileName, element, entries);
    }

    /**
     * Loads the data from the element passed in and creates {@link JsonEntry} for processing
     * later on.
     *
     * @param file    - file the element was read from
     * @param element - the element to process
     * @param entries - list to populate with new entries
     */
    public static void loadJsonElement(String file, JsonElement element, HashMap<String, List<JsonEntry>> entries)
    {
        if (element.isJsonObject())
        {
            JsonObject object = element.getAsJsonObject();
            String author = null;
            String helpSite = null;
            if (object.has("author"))
            {
                JsonObject authorData = object.get("author").getAsJsonObject();
                author = authorData.get("name").getAsString();
                if (authorData.has("site"))
                {
                    helpSite = authorData.get("site").getAsString();
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
            {
                if (!entry.getKey().equalsIgnoreCase("author"))
                {
                    String key = entry.getKey();
                    if (key.contains(":"))
                    {
                        key = key.split(":")[0];
                    }
                    JsonEntry jsonEntry = new JsonEntry(key, file, entry.getValue());
                    jsonEntry.author = author;
                    jsonEntry.authorHelpSite = helpSite;
                    List<JsonEntry> list = entries.get(jsonEntry.jsonKey);
                    if (list == null)
                    {
                        list = new ArrayList();
                    }
                    list.add(jsonEntry);
                    entries.put(jsonEntry.jsonKey, list);
                }
            }
        }
    }

    @Override
    public void postInit()
    {
        processEntries();

        for (IJsonGenObject obj : generatedObjects)
        {
            if (obj instanceof IPostInit)
            {
                ((IPostInit) obj).onPostInit();
            }
            if (obj instanceof IRecipeContainer)
            {
                List<IRecipe> recipes = new ArrayList();
                ((IRecipeContainer) obj).genRecipes(recipes);
                for (IRecipe recipe : recipes)
                {
                    if (recipe != null && recipe.getRecipeOutput() != null)
                    {
                        GameRegistry.addRecipe(recipe);
                    }
                }
            }
        }
    }

    /**
     * Takes a string and attempts to convert into into a useable
     * {@link ItemStack}. Does not support NBT due to massive
     * amount of nesting and complexity that NBT can have. If
     * you want to use this try the Json version.
     *
     * @param string - simple string
     * @return ItemStack, or null
     */
    public static ItemStack fromString(String string)
    {
        //TODO move to helper as this will be reused, not just in Json
        if (string.startsWith("item[") || string.startsWith("block["))
        {
            String out = string.substring(string.indexOf("[") + 1, string.length() - 1); //ends ]
            int meta = -1;

            //Meta handling
            if (out.contains("@"))
            {
                String[] split = out.split("@");
                out = split[0];
                try
                {
                    meta = Integer.parseInt(split[1]);
                }
                catch (NumberFormatException e)
                {
                    throw new IllegalArgumentException();
                }
            }

            //Ensure domain, default to MC
            if (!out.contains(":"))
            {
                out = "minecraft:" + out;
            }
            //TODO implement short hand eg cobble - > CobbleStone

            if (string.startsWith("item["))
            {
                Object obj = Item.itemRegistry.getObject(out);
                if (obj instanceof Item)
                {
                    if (meta > -1)
                    {
                        return new ItemStack((Item) obj);
                    }
                    else
                    {
                        return new ItemStack((Item) obj, 1, meta);
                    }
                }
            }
            else
            {
                Object obj = Block.blockRegistry.getObject(out);
                if (obj instanceof Block)
                {
                    if (meta > -1)
                    {
                        return new ItemStack((Block) obj);
                    }
                    else
                    {
                        return new ItemStack((Block) obj, 1, meta);
                    }
                }
            }
        }
        //Ore Names
        else if (OreDictionary.doesOreNameExist(string))
        {
            List<ItemStack> ores = OreDictionary.getOres(string);
            for (ItemStack stack : ores)
            {
                if (stack != null)
                {
                    return stack;
                }
            }
        }
        return null;
    }

    public static ItemStack fromJson(JsonObject json)
    {
        ItemStack output = null;
        String type = json.get("type").getAsString();
        String item = json.get("item").getAsString();

        int meta = -1;
        if (json.has("meta"))
        {
            meta = json.get("meta").getAsInt();
        }

        if (type.equalsIgnoreCase("block"))
        {
            Object obj = Item.itemRegistry.getObject(item);
            if (obj instanceof Block)
            {
                if (meta > -1)
                {
                    output = new ItemStack((Block) obj);
                }
                else
                {
                    output = new ItemStack((Block) obj, 1, meta);
                }
            }
        }
        else if (type.equalsIgnoreCase("item"))
        {
            Object obj = Item.itemRegistry.getObject(item);
            if (obj instanceof Item)
            {
                if (meta > -1)
                {
                    return new ItemStack((Item) obj);
                }
                else
                {
                    return new ItemStack((Item) obj, 1, meta);
                }
            }
        }
        else if (type.equalsIgnoreCase("dict"))
        {
            List<ItemStack> ores = OreDictionary.getOres(item);
            for (ItemStack stack : ores)
            {
                if (stack != null)
                {
                    output = stack;
                    break;
                }
            }
        }

        if (output != null && json.has("nbt"))
        {
            NBTTagCompound tag = new NBTTagCompound();
            processNBTTagCompound(json.getAsJsonObject("nbt"), tag);
        }
        return output;
    }

    /**
     * Loads NBT data from a json object
     *
     * @param json - json object, converted to entry set
     * @param tag  - tag to save the data to
     */
    public static void processNBTTagCompound(JsonObject json, NBTTagCompound tag)
    {
        for (Map.Entry<String, JsonElement> entry : json.entrySet())
        {
            if (entry.getValue().isJsonPrimitive())
            {
                JsonPrimitive primitive = entry.getValue().getAsJsonPrimitive();
                if (primitive.isBoolean())
                {
                    tag.setBoolean(entry.getKey(), primitive.getAsBoolean());
                }
                else if (primitive.isNumber())
                {
                    tag.setInteger(entry.getKey(), primitive.getAsInt());
                }
                else if (primitive.isString())
                {
                    tag.setString(entry.getKey(), primitive.getAsString());
                }
            }
            else if (entry.getValue().isJsonObject())
            {
                JsonObject object = entry.getValue().getAsJsonObject();
                if (object.has("type"))
                {
                    String type = object.get("type").getAsString();
                    if (type.equalsIgnoreCase("tagCompound"))
                    {
                        NBTTagCompound nbt = new NBTTagCompound();
                        processNBTTagCompound(object, nbt);
                        tag.setTag(entry.getKey(), nbt);
                    }
                    else if (type.equalsIgnoreCase("int"))
                    {
                        tag.setInteger(entry.getKey(), entry.getValue().getAsInt());
                    }
                    else if (type.equalsIgnoreCase("double"))
                    {
                        tag.setDouble(entry.getKey(), entry.getValue().getAsDouble());
                    }
                    else if (type.equalsIgnoreCase("float"))
                    {
                        tag.setFloat(entry.getKey(), entry.getValue().getAsFloat());
                    }
                    else if (type.equalsIgnoreCase("byte"))
                    {
                        tag.setByte(entry.getKey(), entry.getValue().getAsByte());
                    }
                    else if (type.equalsIgnoreCase("short"))
                    {
                        tag.setShort(entry.getKey(), entry.getValue().getAsShort());
                    }
                    else if (type.equalsIgnoreCase("long"))
                    {
                        tag.setLong(entry.getKey(), entry.getValue().getAsLong());
                    }
                    //TODO add byte array
                    //TODO add int array
                    //TODO add tag list
                }
                else
                {
                    NBTTagCompound nbt = new NBTTagCompound();
                    processNBTTagCompound(object, nbt);
                    tag.setTag(entry.getKey(), nbt);
                }
            }
        }
    }

    /**
     * Called to trim all data that is no needed outside
     * of the loading phase to free up RAM
     */
    public void clear()
    {
        externalFiles.clear();
        externalJarFiles.clear();
        classPathResources.clear();
        jsonEntries.clear();
    }

    /**
     * Simple pre-sorter that attempt to place tagged string near the bottom
     * so they are added after tags they depend on.
     */
    public static class StringSortingComparator implements Comparator<String>
    {
        @Override
        public int compare(String o1, String o2)
        {
            if (o1.contains("@") && !o2.contains("@"))
            {
                return 1;
            }
            else if (!o1.contains("@") && o2.contains("@"))
            {
                return -1;
            }
            //TODO attempt to sort using before & after tags
            return o1.compareTo(o2);
        }
    }

    /**
     * Used to store loaded entries during sorting
     */
    public static class JsonEntry
    {
        /** Name of the entry type, used for sorting */
        public final String jsonKey;
        /** Element entry that goes with the name key */
        public final JsonElement element;
        /** File the entry was created from */
        public final String fileReadFrom;

        /** Who create the entry in the file */
        public String author;
        /** Where the error can be reported if the file fails to read */
        public String authorHelpSite;

        public JsonEntry(String jsonKey, String fileReadFrom, JsonElement element)
        {
            this.jsonKey = jsonKey;
            this.fileReadFrom = fileReadFrom;
            this.element = element;
        }

        @Override
        public String toString()
        {
            return jsonKey + "[" + element + "]";
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof JsonEntry)
            {
                return jsonKey.equals(((JsonEntry) object).jsonKey) && element.equals(((JsonEntry) object).element);
            }
            return false;
        }
        //TODO add hashcode
    }
}
