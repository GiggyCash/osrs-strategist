package compass;

import java.util.List;
import net.runelite.api.Skill;

/** Keeps older focused tests concise while production uses one catalog. */
class TrainingMethodDatabase extends TrainingMethodCatalog
{
    public List<TrainingMethod> methodsFor(Skill skill) { return super.legacyFor(skill); }
    @Override public List<TrainingMethod> legacyFor(Skill skill) { return methodsFor(skill); }
    @Override public List<CuratedTrainingMethod> curatedFor(Skill skill) { return java.util.Collections.emptyList(); }
    @Override public List<CuratedTrainingMethod> f2pFor(Skill skill) { return java.util.Collections.emptyList(); }
    @Override boolean legacyOnly() { return true; }
}

class ExpandedTrainingMethodCatalog extends TrainingMethodCatalog
{
    public List<CuratedTrainingMethod> methodsFor(Skill skill) { return curatedFor(skill); }
}

class F2pBaselineMethodCatalog extends TrainingMethodCatalog
{
    public List<CuratedTrainingMethod> methodsFor(Skill skill) { return f2pFor(skill); }
}
