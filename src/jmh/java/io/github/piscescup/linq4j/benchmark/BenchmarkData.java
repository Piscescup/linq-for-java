package io.github.piscescup.linq4j.benchmark;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Provides deterministic test data for LINQ benchmarks.
 *
 * <p>The generated data intentionally contains repeated departments,
 * addresses, cities, education levels and skills so operations such as
 * {@code distinct}, {@code groupBy} and {@code join} can be tested using
 * data distributions closer to real-world applications.</p>
 */
public final class BenchmarkData {

    private static final long RANDOM_SEED = 0x5EED_CAFE_BABEL;

    private static final String[] FIRST_NAMES = {
        "Alice", "Bob", "Charlie", "David", "Emma",
        "Frank", "Grace", "Henry", "Ivy", "Jack",
        "Kevin", "Laura", "Michael", "Nancy", "Oliver",
        "Peter", "Rachel", "Sophia", "Thomas", "Victoria"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown",
        "Jones", "Garcia", "Miller", "Davis",
        "Wilson", "Anderson", "Taylor", "Thomas"
    };

    private static final String[] COUNTRIES = {
        "Singapore",
        "China",
        "Japan",
        "United Kingdom",
        "United States",
        "Australia"
    };

    private static final String[] PROVINCES = {
        "Central",
        "Shanghai",
        "Beijing",
        "Tokyo",
        "London",
        "California",
        "New South Wales"
    };

    private static final String[] CITIES = {
        "Singapore",
        "Shanghai",
        "Beijing",
        "Tokyo",
        "London",
        "San Francisco",
        "Sydney",
        "Shenzhen",
        "Osaka",
        "New York"
    };

    private static final String[] DISTRICTS = {
        "North",
        "South",
        "East",
        "West",
        "Central"
    };

    private static final String[] DEPARTMENT_NAMES = {
        "Platform",
        "Backend",
        "Frontend",
        "Infrastructure",
        "Research",
        "Finance",
        "Human Resources",
        "Marketing",
        "Sales",
        "Operations"
    };

    private static final String[] DIVISIONS = {
        "Engineering",
        "Research",
        "Corporate",
        "Commercial",
        "Operations"
    };

    private static final String[] SKILL_POOL = {
        "Java",
        "Python",
        "C++",
        "Kotlin",
        "SQL",
        "Spring",
        "Docker",
        "Kubernetes",
        "React",
        "TypeScript",
        "Machine Learning",
        "Linux"
    };

    private BenchmarkData() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates deterministic person data.
     */
    public static List<Person> createPeople(int size) {

        Random random = new Random(RANDOM_SEED + size);

        List<Department> departments = createDepartments();

        List<Person> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {

            Department department =
                departments.get(random.nextInt(departments.size()));

            Address address = new Address();

            address.setCountry(
                COUNTRIES[random.nextInt(COUNTRIES.length)]
            );

            address.setProvince(
                PROVINCES[random.nextInt(PROVINCES.length)]
            );

            address.setCity(
                CITIES[random.nextInt(CITIES.length)]
            );

            address.setDistrict(
                DISTRICTS[random.nextInt(DISTRICTS.length)]
            );

            address.setStreet(
                "Street-" + random.nextInt(200)
            );

            address.setPostalCode(
                String.format("%06d", random.nextInt(1_000_000))
            );

            Profile profile = new Profile();

            profile.setPerformanceScore(
                40.0 + random.nextDouble() * 60.0
            );

            profile.setCompletedProjects(
                random.nextInt(40)
            );

            profile.setManager(
                random.nextDouble() < 0.15
            );

            profile.setEducation(
                Education.values()[
                    random.nextInt(Education.values().length)
                    ]
            );

            int skillCount = 1 + random.nextInt(5);

            List<String> skills = new ArrayList<>(skillCount);

            for (int j = 0; j < skillCount; j++) {
                skills.add(
                    SKILL_POOL[
                        random.nextInt(SKILL_POOL.length)
                        ]
                );
            }

            Person person = new Person();

            person.setId(i);

            person.setFirstName(
                FIRST_NAMES[
                    random.nextInt(FIRST_NAMES.length)
                    ]
            );

            person.setLastName(
                LAST_NAMES[
                    random.nextInt(LAST_NAMES.length)
                    ]
            );

            person.setAge(
                18 + random.nextInt(48)
            );

            person.setGender(
                Gender.values()[
                    random.nextInt(Gender.values().length)
                    ]
            );

            person.setAddress(address);
            person.setDepartment(department);

            person.setSalary(
                3_000.0 + random.nextDouble() * 27_000.0
            );

            person.setActive(
                random.nextDouble() < 0.85
            );

            person.setExperienceYears(
                random.nextInt(30)
            );

            person.setSkills(skills);
            person.setProfile(profile);

            result.add(person);
        }

        return result;
    }

    public static List<DepartmentBudget> createDepartmentBudgets() {

        List<Department> departments = createDepartments();

        List<DepartmentBudget> result =
            new ArrayList<>(departments.size());

        for (Department department : departments) {

            DepartmentBudget budget = new DepartmentBudget();

            budget.setDepartmentId(department.getId());

            budget.setAnnualBudget(
                500_000.0
                    + department.getId() * 375_000.0
            );

            budget.setCostCenter(
                "CC-" + String.format(
                    "%04d",
                    department.getId()
                )
            );

            budget.setEmployeeLimit(
                50 + department.getId() * 25
            );

            result.add(budget);
        }

        return result;
    }

    public static List<String> createDistinctKeys(
        List<Person> people
    ) {

        List<String> result =
            new ArrayList<>(people.size());

        for (Person person : people) {

            result.add(
                person.getDepartment().getDivision()
                    + ':'
                    + person.getAddress().getCity()
                    + ':'
                    + person.getProfile().getEducation()
            );
        }

        return result;
    }

    private static List<Department> createDepartments() {

        List<Department> result =
            new ArrayList<>(DEPARTMENT_NAMES.length);

        for (int i = 0; i < DEPARTMENT_NAMES.length; i++) {

            Department department = new Department();

            department.setId(i);

            department.setName(
                DEPARTMENT_NAMES[i]
            );

            department.setDivision(
                DIVISIONS[i % DIVISIONS.length]
            );

            department.setLevel(
                1 + i % 5
            );

            result.add(department);
        }

        return result;
    }

    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    public enum Education {
        HIGH_SCHOOL,
        BACHELOR,
        MASTER,
        DOCTORATE
    }

    public static class Person {

        private long id;

        private String firstName;

        private String lastName;

        private int age;

        private Gender gender;

        private Address address;

        private Department department;

        private double salary;

        private boolean active;

        private int experienceYears;

        private List<String> skills;

        private Profile profile;

        public String getFullName() {
            return firstName + ' ' + lastName;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Gender getGender() {
            return gender;
        }

        public void setGender(Gender gender) {
            this.gender = gender;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getExperienceYears() {
            return experienceYears;
        }

        public void setExperienceYears(int experienceYears) {
            this.experienceYears = experienceYears;
        }

        public List<String> getSkills() {
            return skills;
        }

        public void setSkills(List<String> skills) {
            this.skills = skills;
        }

        public Profile getProfile() {
            return profile;
        }

        public void setProfile(Profile profile) {
            this.profile = profile;
        }
    }

    public static class Address {

        private String country;
        private String province;
        private String city;
        private String district;
        private String street;
        private String postalCode;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }

    public static class Department {

        private int id;
        private String name;
        private String division;
        private int level;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDivision() {
            return division;
        }

        public void setDivision(String division) {
            this.division = division;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }
    }

    public static class Profile {

        private double performanceScore;

        private int completedProjects;

        private boolean manager;

        private Education education;

        public double getPerformanceScore() {
            return performanceScore;
        }

        public void setPerformanceScore(
            double performanceScore
        ) {
            this.performanceScore = performanceScore;
        }

        public int getCompletedProjects() {
            return completedProjects;
        }

        public void setCompletedProjects(
            int completedProjects
        ) {
            this.completedProjects = completedProjects;
        }

        public boolean isManager() {
            return manager;
        }

        public void setManager(boolean manager) {
            this.manager = manager;
        }

        public Education getEducation() {
            return education;
        }

        public void setEducation(Education education) {
            this.education = education;
        }
    }

    public static class DepartmentBudget {

        private int departmentId;

        private double annualBudget;

        private String costCenter;

        private int employeeLimit;

        public int getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(int departmentId) {
            this.departmentId = departmentId;
        }

        public double getAnnualBudget() {
            return annualBudget;
        }

        public void setAnnualBudget(double annualBudget) {
            this.annualBudget = annualBudget;
        }

        public String getCostCenter() {
            return costCenter;
        }

        public void setCostCenter(String costCenter) {
            this.costCenter = costCenter;
        }

        public int getEmployeeLimit() {
            return employeeLimit;
        }

        public void setEmployeeLimit(int employeeLimit) {
            this.employeeLimit = employeeLimit;
        }
    }
}