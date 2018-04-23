package ru.ifmo.ctddev.isaev

/**
 * @author iisaev
 */
enum class KnownDatasets(name: String) {
    ARIZONA5("arizona5_norm_ova_discrete.csv"),
    ARIZONA1("arizona1_mul4_norm_2_ova_discrete.csv"),
    BREAST("Breast(97,24481)_norm_ova_discrete.csv"),
    CNS("CNS(60,7129)_norm_ova_discrete.csv"),
    DATA2_0("data2_train(105,22283)_norm_0_ova_discrete.csv"),
    DATA2_1("data2_train(105,22283)_norm_1_ova_discrete.csv"),
    DATA4("data4_train(113,54675)_norm_2_ova_discrete.csv"),
    DATA5("data5_train(89,54613)_norm_2_ova_discrete.csv"),
    DATA6("data6_train(92,59004)_norm_3_ova_discrete.csv"),
    DLBCL("DLBCL_norm_ova_discrete.csv"),
    GDS2771("GDS2771_norm_ova_discrete.csv"),
    GDS2819_0("GDS2819_norm_0_ova_discrete.csv"),
    GDS2819_1("GDS2819_norm_1_ova_discrete.csv"),
    GDS2819_2("GDS2819_norm_2_ova_discrete.csv"),
    GDS2901("GDS2901_norm_ova_discrete.csv"),
    GDS2947("GDS2947_norm_ova_discrete.csv"),
    GDS2960("GDS2960_norm_ova_discrete.csv"),
    GDS2961("GDS2961_norm_ova_discrete.csv"),
    GDS2962("GDS2962_norm_ova_discrete.csv"),
    GDS3116("GDS3116_norm_ova_discrete.csv"),
    GDS3145("GDS3145_norm_ova_discrete.csv"),
    GDS3244("GDS3244_norm_ova_discrete.csv"),
    GDS3257("GDS3257_norm_ova_discrete.csv"),
    GDS3553("GDS3553_norm_ova_discrete.csv"),
    GDS3622("GDS3622_norm_ova_discrete.csv"),
    GDS3929("GDS3929_norm_ova_discrete.csv"),
    GDS3995("GDS3995_norm_ova_discrete.csv"),
    GDS4103("GDS4103_norm_ova_discrete.csv"),
    GDS4109("GDS4109_norm_ova_discrete.csv"),
    GDS4129("GDS4129_norm_ova_discrete.csv"),
    GDS4130("GDS4130_norm_ova_discrete.csv"),
    GDS4222("GDS4222_norm_ova_discrete.csv"),
    //GDS4261("GDS4261_norm_ova_discrete.csv"), bad dataset
    GDS4318("GDS4318_norm_1_ova_discrete.csv"),
    GDS4336("GDS4336_norm_ova_discrete.csv"),
    GDS4431("GDS4431_norm_ova_discrete.csv"),
    GDS4600("GDS4600_norm_ova_discrete.csv"),
    GDS4837_1("GDS4837_mul3_norm_1_ova_discrete.csv"),
    GDS4837_3("GDS4837_mul3_norm_3_ova_discrete.csv"),
    GDS4901("GDS4901_norm_ova_discrete.csv"),
    GDS4968_0("GDS4968_mul4_norm_0_ova_discrete.csv"),
    GDS4968_1("GDS4968_mul4_norm_1_ova_discrete.csv"),
    GDS5037_0("GDS5037_mul3_norm_0_ova_discrete.csv"),
    GDS5037_2("GDS5037_mul3_norm_2_ova_discrete.csv"),
    GDS5047("GDS5047_norm_ova_discrete.csv"),
    GDS5083("GDS5083_norm_ova_discrete.csv"),
    LEUKEMIA_0("Leukemia_3c(72,7129)_norm_0_ova_discrete.csv"),
    LEUKEMIA_1("Leukemia_3c(72,7129)_norm_1_ova_discrete.csv"),
    OVARIAN("Ovarian(253,15154)_norm_ova_discrete.csv"),
    PLYSRBCT_0("plySRBCT_mul3_norm_0_ova_discrete.csv"),
    PLYSRBCT_1("plySRBCT_mul3_norm_1_ova_discrete.csv"),
    PROSTATE("prostate_tumor_norm_ova_discrete.csv");

    private val path = "/Users/iisaev/Temp/Datasets/Stage5_ololo/" + name

    fun read(): FeatureDataSet {
        return DataSetReader().readCsv(path)
    }
}