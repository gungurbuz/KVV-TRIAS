# TRIAS Departure Board

Get real time departure info for a single platform at a single stop using the KVV's TRIAS API.

---

## Requirements

- Java 25 or newer
- Maven 3.9+
- Internet access
- TRIAS API access (KVV)


---

## Setup

### 1. Clone the repository
``` 
git clone https://github.com/gungurbuz/triastest.git

cd triastest
```

### 2. Set the API key

The application expects the TRIAS API key as an environment variable.

#### Windows (persistent)
Add a user environment variable:
```
TRIAS_API_KEY=your-key
```

### 3. Build
```
mvn clean package
```



## Usage

### Run
```
java -jar target/departure-board.jar
```

Updates immediately and then every minute on the dot.
Both scheduled and expected/realtime information is shown. If the times are identical, only one time is shown.

## Configuration

Edit `DepartureBoardApp` to change:

### Stop
```
STOP_POINT_REF
```

### Lines to display

```
ALLOWED_LINES
```
Example:
```
List.of("S1", "S11")
```

## Acknowledgements

- i love you KVV forever and ever...

## License
This project is licensed under the Creative Commons Zero v1.0 Universal License - see the [LICENSE](LICENSE) file for details.