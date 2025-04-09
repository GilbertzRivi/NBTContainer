# NBTContainer & ICustomNBTSerializable

This package offers a compact solution to serialize and deserialize custom objects by leveraging a key–value container. It is designed to work with any class that provides its own serialization logic via the `ICustomNBTSerializable` interface.

## Components

### NBTContainer
- **Purpose:**  
  Manages a map of custom-serializable objects, keyed by strings.
  
- **Key Features:**  
  - **Serialization:** Converts the container’s entries into a single byte array (or string) with an option for compression.
  - **Custom Delimiters:** Uses 0x01 as field separator and 0x02 as entry separator, ensuring fields (key, type, payload) are properly isolated.
  - **Reflection Based:** Stores fully qualified class names; during deserialization it uses reflection (assuming a default constructor) to instantiate objects.
  - **Utility Methods:** Includes operations such as `set`, `get`, `del`, `clear`, and stream conversion.

### ICustomNBTSerializable
- **Purpose:**  
  Defines the contract that any custom serializable object must follow.
  
- **Interface Methods:**  
  - `String serialize()`: Returns the object’s state as a string.
  - `void deserialize(String data)`: Reconstructs the object’s state from the string.
  
- **Separator Constant:**  
  Provides a constant `SEP` (set to `"|"`) for use within implementations if needed.

## How It Works

### Serialization Process
1. **Setting Values:**  
   Use `NBTContainer.set(key, value)` to add data. The `value` must implement `ICustomNBTSerializable`.
2. **Building the Output:**  
   For each entry, the container writes out:
   - The key (properly escaped).
   - The object’s fully qualified class name.
   - The payload produced by `serialize()`.
3. **Optional Compression:**  
   When enabled, the raw byte array is compressed using Java’s `Deflater` and Base64-encoded when converting to a string.

### Deserialization Process
1. **Data Input:**  
   Provide the saved byte array (or its string representation) back to the container.
2. **Decompression and Parsing:**  
   The stored data is decompressed (if necessary) and split into entries by the defined separators.
3. **Reconstruction:**  
   Each entry is processed:
   - The key, class name, and payload are extracted.
   - Reflection instantiates the object.
   - The object’s `deserialize()` method applies the saved state.

## Example Usage

```java
public class SomeClass extends SuperClass {
    public NBTContainer settings = new NBTContainer();

    public void initSettings() {
        settings.set("A", new LogicSetting());
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        settings.deserialize(data.getByteArray("settings"));
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putByteArray("settings", settings.serialize(true));
    }
}
```
### In this example:

    A new NBTContainer is created to handle a collection of settings.

    Each setting (here LogicSetting) must implement ICustomNBTSerializable.

    The loadTag and saveAdditional methods demonstrate how to integrate the container with a tagging system (e.g., for game data or configuration persistence).

## Considerations

    Default Constructor Requirement: All classes used must provide a zero-argument constructor for reflection-based instantiation.

    Versioning & Compatibility: The container does not account for object version changes; updating serialization logic may require careful migration strategies.

    Performance & Security: Reflection and compression can introduce overhead and potential security risks. Evaluate based on your project’s requirements.
