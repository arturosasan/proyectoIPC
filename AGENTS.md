# Running la Safer — Agent Instructions

## Project Overview
- **Type**: Java/JavaFX desktop application (IPC 2026 course project - UPV)
- **Build**: Apache Ant via NetBeans IDE
- **Database**: SQLite (`sportactivity.db` in root)
- **Main entry**: `src/mapademo/MapaDemoApp.java:29`

## Build & Run Commands
```bash
# Build project (NetBeans/Ant)
ant clean && ant build

# Run application
ant run

# Database inspection
sqlite3 sportactivity.db "SELECT * FROM users;"
```

## Architecture
- **Pattern**: MVC with FXML
- **Controllers**: `*Controller.java` files handle UI logic
- **Views**: `.fxml` files (declarative layouts)
- **Models**: `Poi.java` and domain classes
- **Libraries**: `lib/IPC2026-1.0.0.jar` (course library), `lib/sqlite-jdbc-*.jar`

## Key Files
| Path | Purpose |
|------|---------|
| `build.xml` | Ant build script |
| `nbproject/project.properties` | JavaFX 21 paths, project config |
| `src/mapademo/` | All application source code |
| `maps/` | Map images (JPG) |
| `gpx.zip` | Sample GPX activity files |

## Workflow (GitHub Issues)
1. **Assign issue** before coding
2. **Branch from `main`**: `feature/xxx` or `fix/xxx`
3. **Never commit directly to `main`**
4. **PR closes issue**: use `Closes #N` in PR description

## Quirks & Gotchas
- **JavaFX 21**: External SDK path in `nbproject/project.properties` (local to each dev)
- **SQLite DB**: Auto-created in root; inspect with `sqlite3` CLI
- **NetBeans IDE**: Primary development environment (Git integration via Team menu)
- **No automated tests**: `test/` directory exists but is empty

## Features Implemented
- User registration/login/profile (Categoría 1)
- GPX activity import & visualization (Categoría 2)
- Map zoom/annotations (Categoría 3)
- Elevation profile & speed visualization (Categoría 4)
- Custom map addition (Categoría 5)