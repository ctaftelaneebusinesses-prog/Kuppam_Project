# Android asset licences

Every font and icon shipped in the Android app, with its licence. Add a row before adding an asset.

| Asset | Where | Licence | Source |
|---|---|---|---|
| Inter (variable) | `android/core/designsystem/src/main/res/font/inter.ttf` | SIL Open Font License 1.1 — `docs/font-licenses/Inter-OFL.txt` | https://github.com/google/fonts/tree/main/ofl/inter |
| Doto (variable, dot-matrix) | `android/core/designsystem/src/main/res/font/doto.ttf` | SIL Open Font License 1.1 — `docs/font-licenses/Doto-OFL.txt` | https://github.com/google/fonts/tree/main/ofl/doto |
| Category icons (`ic_bi_*.xml`) | `android/core/designsystem/src/main/res/drawable/` | MIT | Bootstrap Icons 1.11.3, https://icons.getbootstrap.com |

## Not used, on purpose
- **Ndot-57** (Nothing's dot-matrix typeface) is proprietary. The community mirror that was suggested declares no licence,
  so it is not included. Doto fills the same role; to adopt a properly licensed Ndot later, replace `doto.ttf` and update
  this table.

The OFL allows bundling and embedding in an app; it forbids selling the font on its own and using a Reserved Font Name for
modified versions. We ship the fonts unmodified.
