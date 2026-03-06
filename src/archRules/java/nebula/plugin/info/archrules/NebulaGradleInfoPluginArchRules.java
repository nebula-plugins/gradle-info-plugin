package nebula.plugin.info.archrules;

import com.netflix.nebula.archrules.core.ArchRulesService;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;

import static com.netflix.nebula.archrules.common.CanBeAnnotated.Predicates.deprecated;
import static com.netflix.nebula.archrules.common.CanBeAnnotated.Predicates.deprecatedForRemoval;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.targetOwner;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.is;

@NullMarked
public class NebulaGradleInfoPluginArchRules implements ArchRulesService {
    private static final String thisPackage = "nebula.plugin.info..";
    static final ArchRule DEPRECATED = ArchRuleDefinition.priority(Priority.MEDIUM)
            .noClasses().that(resideOutsideOfPackages(thisPackage))
            .should().dependOnClassesThat(resideInAPackage(thisPackage)
                    .and(are(deprecated())))
            .orShould().accessTargetWhere(targetOwner(resideInAPackage(thisPackage))
                    .and(target(is(deprecated()))))
            .allowEmptyShould(true)
            .because("deprecated APIs will be removed in a future version of nebula-gradle-info");

    static final ArchRule DEPRECATED_FOR_REMOVAL = ArchRuleDefinition.priority(Priority.HIGH)
            .noClasses().that(resideOutsideOfPackages(thisPackage))
            .should().dependOnClassesThat(resideInAPackage(thisPackage)
                    .and(are(deprecatedForRemoval())))
            .orShould().accessTargetWhere(targetOwner(resideInAPackage(thisPackage))
                    .and(target(is(deprecatedForRemoval()))))
            .allowEmptyShould(true)
            .because("deprecated for removal APIs will be removed in the next major version of nebula-gradle-info");

    @Override
    public Map<String, ArchRule> getRules() {
        Map<String, ArchRule> rules = new HashMap<>();
        rules.put("nebula-gradle-info deprecated", DEPRECATED);
        rules.put("nebula-gradle-info deprecated for removal", DEPRECATED_FOR_REMOVAL);
        return rules;
    }
}
