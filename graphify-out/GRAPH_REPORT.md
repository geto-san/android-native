# Graph Report - android-native-master-branch  (2026-08-01)

## Corpus Check
- 170 files · ~110,043 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 866 nodes · 975 edges · 111 communities (47 shown, 64 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 133 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `abd003e7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]
- [[_COMMUNITY_Community 52|Community 52]]
- [[_COMMUNITY_Community 53|Community 53]]
- [[_COMMUNITY_Community 54|Community 54]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]
- [[_COMMUNITY_Community 68|Community 68]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]
- [[_COMMUNITY_Community 75|Community 75]]
- [[_COMMUNITY_Community 76|Community 76]]
- [[_COMMUNITY_Community 77|Community 77]]
- [[_COMMUNITY_Community 78|Community 78]]
- [[_COMMUNITY_Community 79|Community 79]]
- [[_COMMUNITY_Community 80|Community 80]]
- [[_COMMUNITY_Community 81|Community 81]]
- [[_COMMUNITY_Community 82|Community 82]]
- [[_COMMUNITY_Community 83|Community 83]]
- [[_COMMUNITY_Community 84|Community 84]]
- [[_COMMUNITY_Community 85|Community 85]]
- [[_COMMUNITY_Community 86|Community 86]]
- [[_COMMUNITY_Community 87|Community 87]]
- [[_COMMUNITY_Community 88|Community 88]]
- [[_COMMUNITY_Community 89|Community 89]]
- [[_COMMUNITY_Community 91|Community 91]]
- [[_COMMUNITY_Community 103|Community 103]]
- [[_COMMUNITY_Community 104|Community 104]]
- [[_COMMUNITY_Community 105|Community 105]]
- [[_COMMUNITY_Community 106|Community 106]]
- [[_COMMUNITY_Community 107|Community 107]]
- [[_COMMUNITY_Community 108|Community 108]]
- [[_COMMUNITY_Community 109|Community 109]]
- [[_COMMUNITY_Community 110|Community 110]]

## God Nodes (most connected - your core abstractions)
1. `ReportIncidentViewModel` - 17 edges
2. `AuthViewModel` - 15 edges
3. `WildWatchNavHost()` - 15 edges
4. `RangerTrackingViewModel` - 14 edges
5. `RepositoryModule` - 13 edges
6. `Wildlife park management platform — Development & Integration Plan` - 13 edges
7. `DynamicReportViewModel` - 11 edges
8. `AuthRepositoryImpl` - 11 edges
9. `RelevanceEvaluator` - 10 edges
10. `HomeScreen()` - 10 edges

## Surprising Connections (you probably didn't know these)
- `WildWatchNavHost()` --calls--> `CameraCaptureScreen()`  [INFERRED]
  app/src/main/java/com/wildwatch/app/ui/nav/WildWatchNavHost.kt → app/src/main/java/com/wildwatch/app/feature/report/CameraCaptureScreen.kt
- `ReportSelectionScreen()` --calls--> `BackHeader()`  [INFERRED]
  app/src/main/java/com/wildwatch/app/feature/report/ReportSelectionScreen.kt → app/src/main/java/com/wildwatch/app/core/ui/component/Headers.kt
- `WildWatchNavHost()` --calls--> `ReportSubmittedScreen()`  [INFERRED]
  app/src/main/java/com/wildwatch/app/ui/nav/WildWatchNavHost.kt → app/src/main/java/com/wildwatch/app/feature/report/ReportSubmittedScreen.kt
- `WildWatchNavHost()` --calls--> `DynamicReportScreen()`  [INFERRED]
  app/src/main/java/com/wildwatch/app/ui/nav/WildWatchNavHost.kt → app/src/main/java/com/wildwatch/app/feature/report/dynamic/ui/DynamicReportScreen.kt
- `CommunityAlertsScreen()` --calls--> `BackHeader()`  [INFERRED]
  app/src/main/java/com/wildwatch/app/feature/alerts/CommunityAlertsScreen.kt → app/src/main/java/com/wildwatch/app/core/ui/component/Headers.kt

## Communities (111 total, 64 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (37): AlertItem(), CategoryFilter, CommunityAlertsScreen(), EmptyAlertsState(), relativeTime(), MainActivity, HistoryPlaceholderScreen(), ArticleDetailScreen() (+29 more)

### Community 1 - "Community 1"
Cohesion: 0.09
Nodes (19): AuthScreen(), FieldLabel(), PhotoGrid(), SeverityChip(), WildWatchDropdownField(), WildWatchTextField(), BackHeader(), PermissionDialog() (+11 more)

### Community 2 - "Community 2"
Cohesion: 0.07
Nodes (10): GeoLocation, LocationRepository, geocodeAsync(), LocationRepositoryImpl, AttractionType, NationalPark, ParkAttraction, RangerTrackingUiState (+2 more)

### Community 3 - "Community 3"
Cohesion: 0.09
Nodes (7): DashboardUiState, DashboardViewModel, DashboardViewModelTest, ProfileViewModel, ProfileViewModelTest, GetIncidentsUseCase, ObserveUserUseCase

### Community 4 - "Community 4"
Cohesion: 0.15
Nodes (3): DynamicReportUiState, DynamicReportViewModel, FormEngine

### Community 5 - "Community 5"
Cohesion: 0.07
Nodes (6): AuthRepositoryImpl, toDomain(), OfflineAuthRepositoryImpl, IncidentRepositoryImpl, User, UserRole

### Community 6 - "Community 6"
Cohesion: 0.10
Nodes (5): IncidentEntity, IncidentDaoTest, IncidentRemoteDataSourceImpl, IncidentRepositoryImplTest, RemoteIncidentChange

### Community 7 - "Community 7"
Cohesion: 0.09
Nodes (3): ReportIncidentViewModel, ReportUiState, ReportIncidentViewModelTest

### Community 8 - "Community 8"
Cohesion: 0.16
Nodes (3): AuthUiState, AuthViewModel, AuthViewModelTest

### Community 9 - "Community 9"
Cohesion: 0.13
Nodes (4): IncidentRepository, NewIncidentDetails, SyncResult, IncidentSyncWorkerTest

### Community 10 - "Community 10"
Cohesion: 0.13
Nodes (6): AlertRepositoryImpl, AlertViewModel, AlertViewModelTest, AlertEntity, Alert, fromEntity()

### Community 11 - "Community 11"
Cohesion: 0.12
Nodes (15): code:properties (USE_LOCAL_BACKEND=true), 🏃 Getting Started, 🚀 Key Features, Local Backend (Docker), 🎨 Look & Feel, 📡 Monitoring & Reporting, Prerequisites, 📸 Previews (+7 more)

### Community 12 - "Community 12"
Cohesion: 0.12
Nodes (15): AccountManagement, ArticleDetail, Auth, CameraCapture, CommunityAlerts, ConflictReport, IncidentDetail, IncidentHistory (+7 more)

### Community 13 - "Community 13"
Cohesion: 0.18
Nodes (4): IncidentDetailUiState, IncidentDetailViewModel, IncidentDetailViewModelTest, GetIncidentByIdUseCase

### Community 14 - "Community 14"
Cohesion: 0.22
Nodes (13): IncidentListItem(), StatusChip(), QuickReportCard(), AlertDetailContent(), AlertListContent(), buildReportSubtitle(), CommunityAlertsCard(), DetailRow() (+5 more)

### Community 15 - "Community 15"
Cohesion: 0.14
Nodes (13): 10. Practical Notes, 11. Local-first Docker development, 1. Project Intent, 2. Current Tech Stack, 3. Roles & Permissions, 4. Architecture at a Glance, 5. Shared Data Model, 6. Track A — Backend & Shared Services (+5 more)

### Community 16 - "Community 16"
Cohesion: 0.24
Nodes (3): FormSchemas, isSelected(), RelevanceEvaluator

### Community 19 - "Community 19"
Cohesion: 0.36
Nodes (8): fromEntity(), fromFirestoreDocument(), Incident, parseAnswersField(), parseRangerProgress(), parseSeverity(), parseStatus(), parseType()

### Community 20 - "Community 20"
Cohesion: 0.18
Nodes (10): Bridge metadata (cross-system), Collections, Enums (Case-insensitive), `feed`, `incidents`, `notifications`, `parks`, `patrol_logs` (+2 more)

### Community 21 - "Community 21"
Cohesion: 0.27
Nodes (5): Choice, FormViewMode, Question, QuestionType, FormSchemaLoader

### Community 22 - "Community 22"
Cohesion: 0.33
Nodes (9): int, build_schema(), classify_section(), main(), normalize_relevance(), Return section key when label/group marks a new section, else None., split_questions(), to_canonical_question() (+1 more)

### Community 24 - "Community 24"
Cohesion: 0.33
Nodes (6): ArticleEntity, Article, fromEntity(), fromFirestoreDocument(), parsePublishedAt(), parseTheme()

### Community 26 - "Community 26"
Cohesion: 0.18
Nodes (13): DashboardIncidentItem(), DashboardScreen(), EmptyState(), SectionHeader(), StatItem(), StatsSection(), ZoneFilterChips(), ArticleCard() (+5 more)

### Community 29 - "Community 29"
Cohesion: 0.29
Nodes (6): client, configuration_version, project_info, project_id, project_number, storage_bucket

### Community 30 - "Community 30"
Cohesion: 0.29
Nodes (6): database, entities, identityHash, setupQueries, version, formatVersion

### Community 31 - "Community 31"
Cohesion: 0.29
Nodes (6): database, entities, identityHash, setupQueries, version, formatVersion

### Community 32 - "Community 32"
Cohesion: 0.29
Nodes (6): questions, forms, conflict, sighting, schemaVersion, questions

### Community 34 - "Community 34"
Cohesion: 0.29
Nodes (6): database, entities, identityHash, setupQueries, version, formatVersion

### Community 35 - "Community 35"
Cohesion: 0.29
Nodes (6): database, entities, identityHash, setupQueries, version, formatVersion

### Community 36 - "Community 36"
Cohesion: 0.29
Nodes (6): database, entities, identityHash, setupQueries, version, formatVersion

### Community 40 - "Community 40"
Cohesion: 0.40
Nodes (3): FeedRemoteChange, FeedRemoteDataSource, FeedRemoteDataSourceImpl

### Community 48 - "Community 48"
Cohesion: 0.40
Nodes (4): CanonicalChoiceDefinition, CanonicalFormDefinition, CanonicalFormSchema, CanonicalQuestionDefinition

### Community 56 - "Community 56"
Cohesion: 0.29
Nodes (3): CoreHomeInputs, HomeUiState, HomeViewModel

### Community 105 - "Community 105"
Cohesion: 0.29
Nodes (6): database, entities, identityHash, setupQueries, version, formatVersion

## Knowledge Gaps
- **117 isolated node(s):** `int`, `project_number`, `project_id`, `storage_bucket`, `client` (+112 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **64 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `User` connect `Community 5` to `Community 3`, `Community 13`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Why does `ProfileUiState` connect `Community 0` to `Community 3`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `ReportIncidentViewModel` (e.g. with `.setUp()` and `.`state is restored from SavedStateHandle`()`) actually correct?**
  _`ReportIncidentViewModel` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `AuthViewModel` (e.g. with `.`signIn with blank email sets an error without calling the repository`()` and `.`signIn success clears loading and error`()`) actually correct?**
  _`AuthViewModel` has 7 INFERRED edges - model-reasoned connections that need verification._
- **Are the 14 inferred relationships involving `WildWatchNavHost()` (e.g. with `.onCreate()` and `AuthScreen()`) actually correct?**
  _`WildWatchNavHost()` has 14 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `RangerTrackingViewModel` (e.g. with `.createViewModel()` and `.`detectActivePark updates location in state`()`) actually correct?**
  _`RangerTrackingViewModel` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `int`, `Return section key when label/group marks a new section, else None.`, `project_number` to the rest of the system?**
  _118 weakly-connected nodes found - possible documentation gaps or missing edges._