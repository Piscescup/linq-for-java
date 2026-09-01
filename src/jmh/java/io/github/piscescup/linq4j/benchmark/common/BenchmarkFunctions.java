package io.github.piscescup.linq4j.benchmark.common;

import java.util.List;

public final class BenchmarkFunctions {

    private BenchmarkFunctions() {
    }

    public static boolean matchesComplexPredicate(BenchmarkData.Person person) {
        return person.isActive()
            && person.getAge() >= 23
            && person.getSalary() >= 7_000.0
            && person.getExperienceYears() >= 2
            && person.getProfile().getPerformanceScore() >= 60.0;
    }

    public static boolean matchesStrictWherePredicate(BenchmarkData.Person person) {
        return person.isActive()
            && person.getAge() >= 25
            && person.getAge() <= 50
            && person.getSalary() >= 8_000.0
            && person.getExperienceYears() >= 3
            && person.getProfile().getPerformanceScore() >= 65.0
            && person.getProfile().getCompletedProjects() >= 5
            && !person.getSkills().isEmpty();
    }

    public static String complexGroupKey(BenchmarkData.Person person) {
        return person.getDepartment().getDivision() + ':'
            + person.getAddress().getProvince() + ':'
            + person.getProfile().getEducation();
    }

    public static String multiLevelGroupKey(BenchmarkData.Person person) {
        return person.getDepartment().getDivision() + ':'
            + person.getAddress().getProvince() + ':'
            + person.getAge() / 10 + ':'
            + person.getProfile().getEducation();
    }

    public static String complexProjection(BenchmarkData.Person person) {
        return person.getFullName()
            + ':'
            + person.getDepartment().getDivision()
            + ':'
            + person.getAddress().getCity()
            + ':'
            + Math.round(
                person.getSalary()
                    * (1.0 + person.getProfile().getPerformanceScore() / 1000.0)
            );
    }

    public static String summarizeGroup(
        String groupKey,
        List<BenchmarkData.Person> group
    ) {
        int count = group.size();
        double totalSalary = 0.0;
        double totalPerformance = 0.0;

        for (BenchmarkData.Person person : group) {
            totalSalary += person.getSalary();
            totalPerformance += person.getProfile().getPerformanceScore();
        }

        double averagePerformance = count == 0
            ? 0.0
            : totalPerformance / count;

        return groupKey
            + ':'
            + count
            + ':'
            + Math.round(totalSalary)
            + ':'
            + Math.round(averagePerformance * 100.0);
    }
}
