package squidpony.squidmath;

import squidpony.annotation.Beta;

/**
 * Another experimental noise class. Extends PerlinNoise and should have similar
 * quality, but can be faster and has less periodic results. This is still
 * considered experimental because the exact output may change in future
 * versions, along with the scale (potentially) and the parameters it takes. In
 * general, {@link #noise(double, double)} and
 * {@link #noise(double, double, double)} should have similar appearance to
 * {@link PerlinNoise#noise(double, double)} and
 * {@link PerlinNoise#noise(double, double, double)}, but are not forced to a
 * zoomed-in scale like PerlinNoise makes its results, are less likely to repeat
 * sections of noise, and are also somewhat faster (a 20% speedup can be
 * expected over PerlinNoise using those two methods). Sound good? <br>
 * Created by Tommy Ettinger on 12/14/2016.
 */
@Beta
public class WhirlingNoise extends PerlinNoise implements Noise.Noise2D, Noise.Noise3D, Noise.Noise4D {
    public static final WhirlingNoise instance = new WhirlingNoise();

    private static int fastFloor(final double t) {
	return t >= 0 ? (int) t : (int) t - 1;
    }

    private static int fastFloor(final float t) {
	return t >= 0 ? (int) t : (int) t - 1;
    }

    protected static final float root2 = 1.4142135f, root3 = 1.7320508f, root5 = 2.236068f,
	    F2f = 0.5f * (WhirlingNoise.root3 - 1f), G2f = (3f - WhirlingNoise.root3) * 0.16666667f, F3f = 0.33333334f,
	    G3f = 0.16666667f, F4f = (WhirlingNoise.root5 - 1f) * 0.25f, G4f = (5f - WhirlingNoise.root5) * 0.05f,
	    unit1_4f = 0.70710678118f, unit1_8f = 0.38268343236f, unit3_8f = 0.92387953251f;
    protected static final float[][] grad2f = { { 1f, 0f }, { -1f, 0f }, { 0f, 1f }, { 0f, -1f },
	    { WhirlingNoise.unit3_8f, WhirlingNoise.unit1_8f }, { WhirlingNoise.unit3_8f, -WhirlingNoise.unit1_8f },
	    { -WhirlingNoise.unit3_8f, WhirlingNoise.unit1_8f }, { -WhirlingNoise.unit3_8f, -WhirlingNoise.unit1_8f },
	    { WhirlingNoise.unit1_4f, WhirlingNoise.unit1_4f }, { WhirlingNoise.unit1_4f, -WhirlingNoise.unit1_4f },
	    { -WhirlingNoise.unit1_4f, WhirlingNoise.unit1_4f }, { -WhirlingNoise.unit1_4f, -WhirlingNoise.unit1_4f },
	    { WhirlingNoise.unit1_8f, WhirlingNoise.unit3_8f }, { WhirlingNoise.unit1_8f, -WhirlingNoise.unit3_8f },
	    { -WhirlingNoise.unit1_8f, WhirlingNoise.unit3_8f }, { -WhirlingNoise.unit1_8f, -WhirlingNoise.unit3_8f } };
    protected static final float[][] phiGrad2f = { { 1, 0 },
	    { (float) Math.cos(PerlinNoise.phi), (float) Math.sin(PerlinNoise.phi) },
	    { (float) Math.cos(PerlinNoise.phi * 2), (float) Math.sin(PerlinNoise.phi * 2) },
	    { (float) Math.cos(PerlinNoise.phi * 3), (float) Math.sin(PerlinNoise.phi * 3) },
	    { (float) Math.cos(PerlinNoise.phi * 4), (float) Math.sin(PerlinNoise.phi * 4) },
	    { (float) Math.cos(PerlinNoise.phi * 5), (float) Math.sin(PerlinNoise.phi * 5) },
	    { (float) Math.cos(PerlinNoise.phi * 6), (float) Math.sin(PerlinNoise.phi * 6) },
	    { (float) Math.cos(PerlinNoise.phi * 7), (float) Math.sin(PerlinNoise.phi * 7) },
	    { (float) Math.cos(PerlinNoise.phi * 8), (float) Math.sin(PerlinNoise.phi * 8) },
	    { (float) Math.cos(PerlinNoise.phi * 9), (float) Math.sin(PerlinNoise.phi * 9) },
	    { (float) Math.cos(PerlinNoise.phi * 10), (float) Math.sin(PerlinNoise.phi * 10) },
	    { (float) Math.cos(PerlinNoise.phi * 11), (float) Math.sin(PerlinNoise.phi * 11) },
	    { (float) Math.cos(PerlinNoise.phi * 13), (float) Math.sin(PerlinNoise.phi * 12) },
	    { (float) Math.cos(PerlinNoise.phi * 13), (float) Math.sin(PerlinNoise.phi * 13) },
	    { (float) Math.cos(PerlinNoise.phi * 14), (float) Math.sin(PerlinNoise.phi * 14) },
	    { (float) Math.cos(PerlinNoise.phi * 15), (float) Math.sin(PerlinNoise.phi * 15) }, };
    /**
     * The 32 3D vertices of a rhombic triacontahedron. These specific values were
     * taken from Vladimir Bulatov's stellation applet, which has available source
     * but is unlicensed, and is <a href=
     * "http://www.bulatov.org/polyhedra/stellation_applet/index.html">available
     * here</a>, but the vertices are mathematical constants so copyright isn't an
     * issue.
     */
    protected static final float[][] grad3f = { { -0.324919696232904f, 0.850650808352036f, 0.000000000000001f },
	    { 0.000000000000001f, 0.850650808352035f, 0.525731112119131f },
	    { 0.324919696232906f, 0.850650808352036f, 0.000000000000001f },
	    { 0.000000000000001f, 0.850650808352036f, -0.525731112119131f },
	    { -0.525731112119131f, 0.525731112119132f, -0.525731112119130f },
	    { -0.850650808352035f, 0.525731112119132f, 0.000000000000001f },
	    { -0.525731112119130f, 0.525731112119131f, 0.525731112119132f },
	    { 0.525731112119132f, 0.525731112119131f, 0.525731112119131f },
	    { 0.850650808352036f, 0.525731112119132f, 0.000000000000000f },
	    { 0.525731112119132f, 0.525731112119132f, -0.525731112119131f },
	    { -0.525731112119132f, 0.000000000000002f, -0.850650808352036f },
	    { -0.850650808352036f, 0.000000000000002f, -0.324919696232905f },
	    { 0.000000000000000f, 0.324919696232906f, -0.850650808352037f },
	    { -0.525731112119131f, 0.000000000000001f, 0.850650808352037f },
	    { 0.000000000000001f, 0.324919696232905f, 0.850650808352037f },
	    { -0.850650808352037f, 0.000000000000001f, 0.324919696232905f },
	    { 0.525731112119133f, 0.000000000000001f, 0.850650808352036f },
	    { 0.850650808352037f, 0.000000000000001f, 0.324919696232905f },
	    { 0.525731112119132f, 0.000000000000001f, -0.850650808352038f },
	    { 0.850650808352038f, 0.000000000000001f, -0.324919696232906f },
	    { -0.525731112119134f, -0.525731112119130f, -0.525731112119133f },
	    { -0.850650808352038f, -0.525731112119130f, -0.000000000000001f },
	    { -0.000000000000001f, -0.324919696232905f, -0.850650808352038f },
	    { -0.000000000000001f, -0.324919696232905f, 0.850650808352038f },
	    { -0.525731112119132f, -0.525731112119131f, 0.525731112119133f },
	    { 0.525731112119133f, -0.525731112119131f, 0.525731112119134f },
	    { 0.850650808352039f, -0.525731112119130f, 0.000000000000001f },
	    { 0.525731112119132f, -0.525731112119134f, -0.525731112119133f },
	    { -0.000000000000003f, -0.850650808352038f, -0.525731112119134f },
	    { -0.324919696232908f, -0.850650808352038f, -0.000000000000002f },
	    { -0.000000000000002f, -0.850650808352042f, 0.525731112119130f },
	    { 0.324919696232902f, -0.850650808352041f, 0.000000000000002f } };
    protected static final float[][] grad4f = { { 0.30521256f, -0.57729936f, 0.74470073f, -0.1378101f },
	    { -0.5383342f, 0.3682797f, -0.23693705f, 0.72001886f },
	    { 0.31043372f, -0.5214427f, 0.7939059f, -0.037971545f },
	    { -0.34612554f, -0.7475893f, -0.14619884f, -0.5476616f },
	    { 0.110175654f, -0.16491795f, 0.98012143f, -0.0050395187f },
	    { -0.49306902f, 0.4408067f, -0.14981277f, 0.7349344f },
	    { 0.2921113f, -0.32707158f, -0.8987039f, 0.0051501133f },
	    { -0.24382289f, -0.54576886f, 0.71103907f, -0.3702839f },
	    { 0.42851725f, -0.3674791f, 0.8249214f, 0.028925087f },
	    { 0.6411811f, -0.6337662f, -0.070845254f, -0.42685845f },
	    { -0.85398096f, -0.1840277f, 0.4859245f, 0.026975961f },
	    { -0.37726673f, 0.4666541f, -0.096711665f, 0.79407215f },
	    { 0.16541202f, 0.60646987f, -0.3924725f, -0.67141527f },
	    { -0.2106679f, 0.30791336f, 0.799212f, 0.4712416f }, { 0.4371983f, -0.26759f, 0.8468081f, 0.1420189f },
	    { -0.20061846f, 0.31516862f, -0.85302573f, -0.3643735f },
	    { -0.43534753f, -0.06332874f, 0.8964169f, 0.05384f },
	    { -0.3837288f, 0.6487092f, -0.0042441157f, -0.657199f },
	    { 0.19373763f, -0.061797444f, -0.9769146f, 0.06545838f },
	    { -0.7183736f, 0.23978147f, 0.6111837f, -0.2299971f },
	    { 0.2960002f, -0.08083333f, -0.45766684f, -0.8345004f },
	    { -0.25097045f, -0.80887955f, 0.026866198f, -0.5310422f },
	    { -0.53422993f, 0.7232398f, 0.4211347f, 0.11903002f },
	    { 0.55193734f, -0.36371425f, -0.61939865f, 0.42358282f },
	    { -0.43355975f, 0.63322467f, -0.30332342f, -0.5648428f },
	    { 0.71517783f, 0.38801393f, 0.060850836f, 0.5781548f },
	    { 0.2987888f, -0.82676244f, 0.45530754f, 0.14101125f },
	    { -0.13143522f, 0.43352097f, -0.8460369f, -0.28107986f },
	    { 0.27406123f, -0.0061683212f, 0.43692502f, 0.85670817f },
	    { -0.17194793f, 0.82895166f, 0.16079327f, -0.5073643f },
	    { 0.26733503f, 0.0014139203f, -0.9508974f, 0.15596196f },
	    { -0.063544184f, -0.33980247f, 0.7871041f, 0.51084584f },
	    { 0.45355868f, 0.013269774f, -0.39320785f, -0.79968494f },
	    { -0.14271542f, -0.84682304f, 0.2664987f, -0.43760887f },
	    { -0.631048f, 0.06282108f, 0.7201872f, 0.28135782f },
	    { 0.6872453f, -0.33575317f, -0.6214131f, -0.16973351f },
	    { -0.7943215f, 0.09599423f, -0.46707594f, 0.3764021f },
	    { -0.02583059f, 0.5708325f, 0.1725252f, 0.8023204f },
	    { 0.36104593f, -0.7183975f, -0.2417964f, -0.54321754f },
	    { -0.62640476f, -0.24817976f, 0.727577f, -0.12905663f },
	    { 0.5240908f, 0.09631077f, 0.77270806f, 0.344928f },
	    { -0.004417861f, -0.43781736f, -0.8712169f, -0.2219855f },
	    { -0.52296543f, 0.10600555f, 0.76990277f, 0.35002825f },
	    { 0.014057253f, -0.8063729f, 0.4908759f, -0.32955426f },
	    { 0.5829141f, 0.15523833f, -0.3080137f, -0.73569f },
	    { 0.013204535f, -0.28376737f, 0.9523351f, -0.1111738f },
	    { -0.720573f, 0.23616794f, -0.3689131f, 0.5374964f },
	    { 0.060347114f, 0.64367414f, -0.7508531f, -0.13513494f },
	    { -0.28348896f, 0.7306394f, 0.4869968f, -0.38553104f },
	    { 0.060365494f, -0.2707644f, -0.6018939f, 0.7488434f },
	    { -0.5243532f, 0.22156887f, -0.27637747f, -0.7743232f },
	    { -0.5711214f, 0.43126655f, 0.19044453f, -0.67198247f },
	    { -0.31680956f, -0.62567914f, 0.6342563f, 0.32538632f },
	    { 0.06309463f, 0.48411465f, -0.46944404f, -0.73571354f },
	    { -0.25846493f, 0.1508792f, -0.108005695f, 0.94803274f },
	    { 0.29359144f, -0.6216346f, 0.7143707f, -0.13057186f },
	    { 0.30669925f, 0.11621855f, -0.5659279f, -0.7564089f },
	    { -0.4579672f, -0.12977019f, 0.71692353f, 0.5093588f },
	    { 0.72682136f, 0.2829506f, -0.16092123f, -0.60479254f },
	    { 0.094798625f, -0.14094533f, -0.9851869f, -0.023545148f },
	    { -0.19733784f, 0.7687776f, 0.5319371f, 0.2950961f },
	    { 0.8743509f, -0.1656985f, -0.45611945f, 0.0030883756f },
	    { -0.33973676f, -0.7613095f, -0.12979484f, 0.5367868f },
	    { -0.66581863f, 0.66475755f, 0.33856988f, 0.012382605f },
	    { 0.6316928f, -0.62325597f, -0.06916507f, 0.4557767f },
	    { -0.86808014f, -0.18651712f, 0.45919082f, 0.028143227f },
	    { 0.7435046f, 0.33720765f, -0.069088265f, 0.57334f },
	    { 0.17722823f, -0.89015186f, -0.41887683f, 0.02760538f },
	    { -0.40754265f, 0.54529834f, -0.09077789f, -0.7268549f },
	    { 0.4377619f, -0.26609203f, 0.8468283f, 0.14297345f },
	    { -0.2980038f, -0.7838384f, -0.05605936f, -0.5418934f },
	    { 0.41718817f, -0.22535361f, -0.86789536f, 0.14807884f },
	    { -0.31047124f, 0.52717996f, -0.0025840108f, 0.7910008f },
	    { 0.48056296f, -0.20454551f, -0.8370399f, 0.16304804f },
	    { -0.1726224f, -0.48157752f, 0.80440617f, -0.30201873f },
	    { -0.80258685f, -0.09523491f, 0.5749053f, 0.12754847f },
	    { 0.82261246f, 0.421295f, 0.019977508f, -0.38133997f },
	    { -0.7746771f, -0.08136189f, 0.6119533f, 0.13699931f },
	    { -0.2080354f, 0.53238916f, 0.038112316f, 0.8196527f },
	    { 0.3244066f, -0.060978226f, -0.44636208f, -0.8317469f },
	    { -0.22975878f, -0.8135528f, 0.10953062f, -0.5228248f },
	    { -0.7301818f, -0.05867752f, 0.65011483f, 0.20184688f },
	    { -0.08505362f, 0.28215852f, -0.54953223f, -0.78177154f },
	    { -0.7179047f, -0.009138507f, 0.6623404f, 0.21409011f },
	    { -0.2239077f, 0.8207485f, 0.16006742f, -0.5006153f },
	    { 0.26869303f, 0.0018523374f, -0.9537537f, 0.13473849f },
	    { -0.6122548f, 0.2766686f, 0.6212354f, 0.40331778f },
	    { 0.85527843f, 0.008107095f, -0.22845533f, -0.46501747f },
	    { -0.13990346f, -0.83571506f, 0.20660898f, -0.4892035f },
	    { -0.43144038f, 0.7503387f, 0.46641788f, 0.18249781f },
	    { 0.6109145f, -0.29784846f, -0.5516091f, 0.48352572f },
	    { -0.5537214f, 0.034945432f, -0.35802394f, -0.75099283f },
	    { -0.05817811f, 0.5701312f, 0.17276435f, 0.80107313f },
	    { 0.37052378f, -0.7357565f, 0.5243631f, 0.21544799f },
	    { -0.029430708f, 0.3613586f, -0.5237315f, -0.77088195f },
	    { 0.33915582f, 0.06269238f, -0.21377106f, 0.9139721f },
	    { -0.0069569536f, -0.8187294f, 0.3973037f, -0.41446778f },
	    { 0.73752713f, 0.1494311f, -0.4386642f, 0.49122077f },
	    { 0.007840344f, -0.41011772f, 0.25074953f, 0.8768504f },
	    { 0.5866233f, 0.12097919f, -0.3089065f, -0.7387922f },
	    { 0.038346753f, -0.7936974f, 0.5219132f, -0.31012994f },
	    { -0.48829433f, 0.1609236f, 0.77598804f, 0.36539677f },
	    { 0.77487123f, -0.27046844f, -0.56236655f, -0.100822724f },
	    { -0.43294445f, -0.8055559f, -0.21639013f, 0.3417808f },
	    { 0.06327453f, 0.57862145f, 0.23459785f, 0.7785611f },
	    { 0.47261932f, -0.6973982f, -0.18080638f, -0.50751925f },
	    { -0.57226986f, -0.19045952f, 0.79454595f, -0.07020757f },
	    { 0.75739795f, 0.24353026f, -0.2703368f, 0.5421802f },
	    { 0.09329002f, 0.71155983f, -0.6887454f, -0.10300197f },
	    { -0.3835674f, 0.19288845f, 0.80135345f, 0.4165366f },
	    { 0.22881314f, -0.63167685f, 0.72891444f, -0.13157767f },
	    { 0.69621545f, 0.26425454f, -0.20363748f, -0.6355984f },
	    { -0.5298094f, -0.16983661f, 0.83038956f, -0.030177508f },
	    { -0.5203182f, 0.35296547f, -0.19933324f, -0.751632f },
	    { 0.22063582f, -0.3258306f, -0.9177535f, -0.053691696f },
	    { -0.27172714f, -0.5687724f, 0.6676368f, -0.39613563f },
	    { 0.1415665f, -0.18683445f, -0.5152346f, 0.82436955f },
	    { -0.46999532f, 0.40541205f, -0.17897326f, -0.7633571f },
	    { -0.6798877f, 0.65324455f, 0.33296168f, 0.012679209f },
	    { 0.6305932f, -0.6210434f, -0.09748693f, 0.4551413f },
	    { 0.13408749f, 0.5705748f, -0.41801533f, -0.6940664f },
	    { -0.18411608f, 0.20425196f, -0.04136734f, 0.9605577f },
	    { 0.44455504f, -0.36305627f, 0.8158576f, 0.07026641f },
	    { 0.36841255f, 0.17022495f, -0.5395205f, -0.73770815f },
	    { -0.39160624f, -0.07530158f, 0.74071103f, 0.5406676f },
	    { 0.7973144f, 0.3746133f, -0.04808778f, -0.47078887f },
	    { 0.16265045f, -0.087234035f, -0.9821154f, 0.03720781f },
	    { -0.6909652f, -0.34583774f, 0.5445337f, 0.32626122f },
	    { 0.920368f, -0.09085865f, -0.37323156f, 0.073250644f },
	    { -0.28366125f, -0.7928303f, 0.012259258f, -0.53926444f },
	    { -0.56178755f, 0.7167111f, 0.4032907f, 0.08986991f },
	    { 0.55126f, -0.41108644f, -0.6798098f, -0.25490978f },
	    { -0.79137385f, -0.08246414f, 0.5891518f, 0.1408093f },
	    { 0.7238841f, 0.3756506f, 0.027417112f, 0.57803696f },
	    { 0.27134395f, -0.8808191f, -0.37211046f, 0.10983609f },
	    { -0.26500604f, 0.7478573f, 0.07171241f, -0.60443246f },
	    { 0.4955693f, -0.073575415f, 0.82634485f, 0.25720012f },
	    { -0.21931332f, -0.8208177f, 0.08023434f, -0.52127004f },
	    { 0.59726536f, -0.06780095f, -0.7328664f, 0.3187223f },
	    { -0.15090077f, -0.5983029f, 0.1091213f, 0.77932996f },
	    { 0.34171078f, 0.84924453f, -0.36408985f, 0.17162769f },
	    { -0.09574758f, -0.4046576f, 0.8811375f, -0.22512527f },
	    { -0.689211f, 0.016008975f, 0.68412775f, 0.23811999f },
	    { -0.07317587f, 0.4764447f, -0.8375025f, -0.25736237f },
	    { -0.51277226f, 0.021356434f, 0.5554851f, -0.6542513f },
	    { -0.07586732f, -0.5166669f, 0.14364976f, 0.8406332f },
	    { 0.4579639f, 0.038025483f, -0.38191295f, -0.8018514f },
	    { -0.103942715f, -0.82185036f, 0.3132923f, -0.4643337f },
	    { -0.5998191f, 0.051231254f, 0.73843443f, 0.30382064f },
	    { -0.029040387f, 0.3618993f, -0.5235325f, -0.77077836f },
	    { -0.2932128f, -0.50995934f, 0.3741883f, 0.7169036f },
	    { 0.88662016f, -0.3880005f, 0.17477578f, -0.18114574f },
	    { 0.34472477f, 0.070200406f, -0.9129205f, 0.20691285f },
	    { -0.5353782f, 0.31744298f, 0.6480155f, 0.43894878f },
	    { 0.5632882f, 0.12426364f, -0.31476036f, -0.75378436f },
	    { 0.020805571f, -0.4152704f, -0.88762134f, -0.19810592f },
	    { -0.33548284f, 0.76334757f, 0.49935544f, 0.23536326f },
	    { 0.65878147f, -0.22940378f, -0.47741848f, 0.5342775f },
	    { -0.53736955f, 0.15640032f, -0.30673102f, -0.76986295f },
	    { 0.055417743f, 0.91139966f, 0.37004194f, -0.17131379f },
	    { 0.5169996f, -0.7614511f, -0.19698289f, 0.33778897f },
	    { -0.56972605f, 0.4115734f, 0.17101042f, -0.69048893f },
	    { 0.75742745f, 0.24410513f, -0.26921594f, 0.5424381f },
	    { 0.069828235f, -0.97122616f, 0.2145969f, -0.0761047f },
	    { 0.67210144f, 0.22488956f, -0.2223229f, -0.6695348f },
	    { 0.10232841f, -0.2801582f, 0.32462245f, 0.897586f },
	    { -0.51016176f, 0.27948615f, -0.24764954f, -0.7747852f },
	    { 0.15745331f, -0.37653404f, -0.91052145f, -0.06619125f },
	    { -0.35133055f, 0.23934884f, 0.79895335f, 0.42538512f },
	    { 0.84832406f, -0.19804582f, -0.4902316f, -0.028233642f },
	    { -0.3739194f, -0.7834053f, -0.13593386f, 0.47747502f },
	    { 0.19779265f, 0.8825428f, 0.42543244f, -0.03167734f },
	    { 0.7468733f, 0.30633524f, -0.1342039f, -0.57474196f },
	    { -0.88764054f, -0.2259622f, 0.4009076f, -0.017564435f },
	    { -0.45524576f, 0.4243537f, -0.16282937f, 0.76561207f },
	    { 0.18682976f, 0.7929761f, -0.5798725f, -0.0056133526f },
	    { -0.22737576f, -0.53136384f, 0.70865744f, 0.4046694f },
	    { 0.39352816f, -0.37106863f, 0.8378966f, 0.073300704f },
	    { -0.22612447f, 0.27001774f, -0.8536839f, -0.38364306f },
	    { -0.84863514f, -0.19870402f, 0.48731726f, 0.053450614f },
	    { -0.43688715f, 0.5594756f, -0.07066281f, -0.700802f },
	    { 0.40344277f, -0.2148357f, -0.88454413f, 0.09306541f },
	    { -0.19478711f, -0.48726425f, 0.73013085f, 0.43765336f },
	    { 0.9219448f, -0.09051001f, -0.37322578f, 0.05028156f },
	    { -0.36481073f, 0.61899686f, -0.03775136f, -0.6945004f },
	    { -0.81090957f, -0.12670651f, 0.5574955f, 0.12477961f },
	    { 0.68599266f, -0.51061195f, -0.010075784f, 0.51825476f },
	    { 0.2066314f, 0.65144634f, -0.35600528f, -0.6373236f },
	    { -0.11573376f, 0.24965633f, 0.01856937f, 0.9612141f },
	    { 0.51501304f, -0.14515176f, 0.81852716f, 0.20905946f },
	    { -0.106857784f, 0.24998063f, -0.55821455f, -0.78389263f },
	    { -0.4754182f, -0.035513934f, -0.31372645f, 0.82115287f },
	    { -0.24548152f, 0.7675945f, 0.09130737f, -0.584979f },
	    { 0.3544801f, -0.039527472f, -0.43353555f, -0.8275436f },
	    { -0.63865834f, -0.29244968f, 0.6007099f, 0.3817543f },
	    { 0.8324249f, -0.017320063f, -0.2546005f, -0.4918815f },
	    { -0.19354364f, -0.8212621f, 0.17127427f, -0.50865954f },
	    { -0.45606834f, 0.7491067f, 0.45366144f, 0.1582155f },
	    { 0.6473732f, -0.36053053f, -0.6419752f, -0.1969605f },
	    { -0.51058674f, 0.021834841f, 0.5262477f, -0.6796233f },
	    { 0.70401603f, 0.4015401f, 0.09906377f, 0.5773329f },
	    { 0.37328795f, -0.85070235f, -0.31028062f, 0.20171149f },
	    { -0.87026936f, 0.43279198f, 0.1133502f, -0.20609237f },
	    { 0.7032974f, 0.07507336f, -0.5531491f, 0.44018495f },
	    { -0.07064539f, 0.89369893f, 0.23307832f, -0.37681022f },
	    { 0.7119635f, 0.09115333f, -0.5296553f, 0.45195612f }, { -0.038092706f, -0.4684961f, 0.21205f, 0.8567935f },
	    { 0.52836746f, 0.10814144f, -0.34704006f, -0.76726556f },
	    { -0.047906272f, -0.83101475f, 0.4072192f, -0.3758883f },
	    { -0.7631498f, 0.16756177f, -0.42105612f, 0.46069202f },
	    { 0.019005984f, 0.5960935f, -0.78351843f, -0.17438526f },
	    { -0.44480333f, 0.1093444f, 0.66349167f, -0.5915848f },
	    { 0.031733893f, -0.3822001f, 0.23787121f, 0.8923751f },
	    { 0.5928739f, 0.15020806f, -0.29245773f, -0.7351235f },
	    { -0.709945f, -0.25679013f, 0.1963655f, 0.6256816f }, { 0.77101755f, 0.1949857f, -0.33680996f, 0.5040551f },
	    { 0.034284342f, 0.44709295f, -0.48786792f, -0.7489442f },
	    { -0.23434283f, -0.446607f, 0.41925618f, 0.754884f },
	    { 0.9220484f, -0.28314397f, 0.248978f, -0.087556966f },
	    { 0.39542305f, 0.13259506f, -0.8086393f, -0.41492364f },
	    { -0.48490062f, -0.17072529f, 0.70224714f, 0.4925172f },
	    { 0.6847746f, 0.23774141f, -0.20929065f, -0.65632325f },
	    { 0.15174845f, -0.36007288f, -0.91447014f, -0.10518683f },
	    { -0.66364074f, 0.5920555f, 0.40352282f, 0.2149897f },
	    { 0.8487449f, -0.19755904f, -0.48972642f, -0.027757192f },
	    { -0.2725929f, -0.57164645f, 0.64824593f, -0.42271817f },
	    { -0.7204641f, 0.62428117f, 0.30120832f, -0.021864176f },
	    { 0.42192665f, -0.46847096f, -0.71710336f, 0.29711226f },
	    { -0.5278077f, 0.48799595f, 0.23897435f, -0.65281713f },
	    { -0.4915112f, 0.415481f, -0.15813626f, 0.74885595f },
	    { 0.14896473f, -0.9420127f, 0.30067563f, -0.0039662635f },
	    { 0.40879646f, 0.17541802f, -0.05796071f, -0.8937307f },
	    { 0.17770523f, -0.16647717f, 0.37773693f, 0.8933202f },
	    { -0.44056776f, 0.47730166f, -0.13542879f, -0.7481592f },
	    { 0.18475258f, -0.15509109f, -0.49493393f, -0.8347776f },
	    { -0.23192436f, 0.29824346f, 0.80051947f, 0.4652209f },
	    { 0.7728951f, 0.5276986f, -0.35039353f, 0.037305843f },
	    { -0.2907823f, -0.728466f, -0.036409806f, 0.6192393f },
	    { -0.8384365f, -0.14058013f, 0.520564f, 0.0792124f },
	    { 0.54468423f, 0.26451725f, -0.73881125f, -0.29581678f },
	    { -0.8132668f, -0.12647997f, -0.56083083f, 0.08982565f },
	    { -0.27625933f, 0.5097125f, -0.01496917f, 0.8146471f },
	    { 0.27182457f, -0.097434245f, -0.46669307f, -0.83595186f },
	    { -0.266864f, -0.8014467f, -6.421754E-4f, -0.5352255f },
	    { 0.4766369f, -0.14767133f, 0.83947283f, 0.2151646f },
	    { -0.16173786f, 0.380226f, -0.8472566f, -0.33380434f },
	    { -0.46358418f, -0.056809172f, 0.3725527f, 0.8019145f },
	    { 0.8303505f, 0.44172588f, 0.05310078f, -0.33552432f },
	    { 0.5844279f, -0.063998416f, -0.7663617f, 0.25891706f },
	    { -0.11677986f, -0.3966828f, 0.76837516f, 0.48847187f },
	    { 0.6264683f, -0.04814682f, -0.722762f, 0.28780982f },
	    { -0.109266914f, 0.42441288f, -0.851426f, -0.28811142f },
	    { -0.70631105f, -0.019139754f, 0.668031f, 0.23343737f },
	    { 0.71994025f, -0.4001531f, 0.068379015f, 0.5629279f },
	    { -0.5569895f, -0.006145278f, -0.37386537f, -0.7415859f },
	    { -0.17033575f, -0.846144f, 0.22243054f, -0.45337692f }, };
//    public static void randomUnitVector4(int seed, final float[] vector)
//    {
//        double mag = 0.0;
//        float t;
//        vector[0] = (t = NumberTools.formCurvedFloat(seed += 0xCB72F6C7));
//        mag += t * t;
//        vector[1] = (t = NumberTools.formCurvedFloat(seed += 0xCB72F6C7));
//        mag += t * t;
//        vector[2] = (t = NumberTools.formCurvedFloat(seed += 0xCB72F6C7));
//        mag += t * t;
//        vector[3] = (t = NumberTools.formCurvedFloat(seed + 0xCB72F6C7));
//        mag += t * t;
//
//        if(mag == 0)
//        {
//            vector[0] = 1f;
//            mag = 1.0;
//        }
//        else
//            mag = Math.sqrt(mag);
//        vector[0] /= mag;
//        vector[1] /= mag;
//        vector[2] /= mag;
//        vector[3] /= mag;
//    }
    static {
	for (int i = 0; i < 32; i++) {
	    final float x = WhirlingNoise.grad3f[i][0], y = WhirlingNoise.grad3f[i][1], z = WhirlingNoise.grad3f[i][2];
	    final float len = 1f / (float) Math.sqrt(x * x + y * y + z * z),
		    len3 = len * WhirlingNoise.root2 * 0.88888f;
	    // final float len = 2f / Math.max(Math.abs(x), Math.max(Math.abs(y),
	    // Math.abs(z))), len3 = len * 1.5f;
	    WhirlingNoise.grad3f[i][0] *= len3;
	    WhirlingNoise.grad3f[i][1] *= len3;
	    WhirlingNoise.grad3f[i][2] *= len3;
	}
//        for (int i = 0; i < 256; i++) {
//            randomUnitVector4(~i * 0x632BE5AB, grad4f[i]);
//            System.out.println("{" + StringKit.join(", ", grad4f[i]) + "},");
//        }
    }
//    protected static final float[][] phiGrad3f = new float[96][3];
//
//    static {
//        final float root2 = 1.2599211f;
//        int i = 0;
//        for (; i < 16; i++) {
//            phiGrad3f[i][0] = phiGrad2f[i & 15][0] * root2;
//            phiGrad3f[i][1] = phiGrad2f[i & 15][1] * root2;
//        }
//        for (; i < 32; i++) {
//            phiGrad3f[i][0] = phiGrad2f[i & 15][1] * root2;
//            phiGrad3f[i][1] = phiGrad2f[i & 15][0] * root2;
//        }
//        for (; i < 48; i++) {
//            phiGrad3f[i][0] = phiGrad2f[i & 15][0] * root2;
//            phiGrad3f[i][2] = phiGrad2f[i & 15][1] * root2;
//        }
//        for (; i < 64; i++) {
//            phiGrad3f[i][0] = phiGrad2f[i & 15][1] * root2;
//            phiGrad3f[i][2] = phiGrad2f[i & 15][0] * root2;
//        }
//        for (; i < 80; i++) {
//            phiGrad3f[i][1] = phiGrad2f[i & 15][0] * root2;
//            phiGrad3f[i][2] = phiGrad2f[i & 15][1] * root2;
//        }
//        for (; i < 96; i++) {
//            phiGrad3f[i][1] = phiGrad2f[i & 15][1] * root2;
//            phiGrad3f[i][2] = phiGrad2f[i & 15][0] * root2;
//        }
//    }

    protected static float dotf(final float g[], final float x, final float y) {
	return g[0] * x + g[1] * y;
    }

    protected static float dotf(final int g[], final float x, final float y, final float z) {
	return g[0] * x + g[1] * y + g[2] * z;
    }

    protected static float dotf(final float g[], final float x, final float y, final float z) {
	return g[0] * x + g[1] * y + g[2] * z;
    }

    protected static double dot(final float g[], final double x, final double y, final double z) {
	return g[0] * x + g[1] * y + g[2] * z;
    }

    protected static double dot(final float g[], final double x, final double y, final double z, final double w) {
	return g[0] * x + g[1] * y + g[2] * z + g[3] * w;
    }
    /*
     * // makes the noise appear muddled and murky; probably not ideal protected
     * static float dotterize(final float x, final float y, final int i, final int
     * j) { return ZapRNG.randomSignedFloat(i * 0x9E3779B97F4A7C15L, j *
     * 0xC6BC279692B5C483L) * x + ZapRNG.randomSignedFloat(j * 0x8E3779B97F4A7C15L,
     * i * 0x632AE59B69B3C209L) * y; }
     */

    /*
     * public static double interpolate(double t, double low, double high) { //debug
     * //return 0; //linear //return t; //hermite //return t * t * (3 - 2 * t);
     * //quintic //return t * t * t * (t * (t * 6 - 15) + 10);
     *
     * //t = (t + low + 0.5 - high) * 0.5; //t = (t < 0.5) ? t * low : 1.0 - ((1.0 -
     * t) * high); //t = Math.pow(t, 1.0 + high - low);
     *
     * //return (t + 0.5 + high - low) * 0.5; return t * t * t * (t * (t * 6 - 15) +
     * 10); }
     */
    /**
     * 2D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double)} and this method. Roughly 20-25%
     * faster than the equivalent method in PerlinNoise, plus it has less chance of
     * repetition in chunks because it uses a pseudo-random function (curiously,
     * {@link ThrustRNG#determine(long)}, which has rather good distribution and is
     * fast) instead of a number chosen by hash from a single 256-element array.
     *
     * @param x X input; works well if between 0.0 and 1.0, but anything is accepted
     * @param y Y input; works well if between 0.0 and 1.0, but anything is accepted
     * @return noise from -1.0 to 1.0, inclusive
     */
    @Override
    public double getNoise(final double x, final double y) {
	return WhirlingNoise.noise(x, y);
    }

    /**
     * Identical to {@link #getNoise(double, double)}; ignores seed.
     *
     * @param x    X input; works well if between 0.0 and 1.0, but anything is
     *             accepted
     * @param y    Y input; works well if between 0.0 and 1.0, but anything is
     *             accepted
     * @param seed ignored entirely.
     * @return noise from -1.0 to 1.0, inclusive
     */
    @Override
    public double getNoiseWithSeed(final double x, final double y, final int seed) {
	return WhirlingNoise.noise(x, y, seed);
    }

    /**
     * 3D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double, double)} and this method. Roughly
     * 20-25% faster than the equivalent method in PerlinNoise, plus it has less
     * chance of repetition in chunks because it uses a pseudo-random function
     * (curiously, {@link ThrustRNG#determine(long)}, which has rather good
     * distribution and is fast) instead of a number chosen by hash from a single
     * 256-element array.
     *
     * @param x X input
     * @param y Y input
     * @param z Z input
     * @return noise from -1.0 to 1.0, inclusive
     */
    @Override
    public double getNoise(final double x, final double y, final double z) {
	return WhirlingNoise.noise(x, y, z);
    }

    /**
     * Identical to {@link #getNoise(double, double, double)}; ignores seed.
     *
     * @param x    X input
     * @param y    Y input
     * @param z    Z input
     * @param seed ignored entirely.
     * @return noise from -1.0 to 1.0, inclusive
     */
    @Override
    public double getNoiseWithSeed(final double x, final double y, final double z, final int seed) {
	return WhirlingNoise.noise(x, y, z, seed);
    }

    /**
     * 4D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double, double)} and this method. Roughly
     * 20-25% faster than the equivalent method in PerlinNoise, plus it has less
     * chance of repetition in chunks because it uses a pseudo-random function
     * (curiously, {@link ThrustRNG#determine(long)}, which has rather good
     * distribution and is fast) instead of a number chosen by hash from a single
     * 256-element array.
     *
     * @param x X input
     * @param y Y input
     * @param z Z input
     * @param w W input (fourth-dimension)
     * @return noise from -1.0 to 1.0, inclusive
     */
    @Override
    public double getNoise(final double x, final double y, final double z, final double w) {
	return WhirlingNoise.noise(x, y, z, w);
    }

    /**
     * Identical to {@link #getNoise(double, double, double, double)}; ignores seed.
     *
     * @param x    X input
     * @param y    Y input
     * @param z    Z input
     * @param w    W input (fourth-dimension)
     * @param seed ignored entirely.
     * @return noise from -1.0 to 1.0, inclusive
     */
    @Override
    public double getNoiseWithSeed(final double x, final double y, final double z, final double w, final int seed) {
	return WhirlingNoise.noise(x, y, z, w, seed);
    }

    /**
     * 2D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double)} and this method. Roughly 20-25%
     * faster than the equivalent method in PerlinNoise, plus it has less chance of
     * repetition in chunks because it uses a pseudo-random function (curiously,
     * {@link ThrustRNG#determine(long)}, which has rather good distribution and is
     * fast) instead of a number chosen by hash from a single 256-element array.
     *
     * @param xin X input; works well if between 0.0 and 1.0, but anything is
     *            accepted
     * @param yin Y input; works well if between 0.0 and 1.0, but anything is
     *            accepted
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static double noise(final double xin, final double yin) {
	return WhirlingNoise.noise(xin, yin, 123456789);
    }

    /**
     * 2D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double)} and this method. Roughly 20-25%
     * faster than the equivalent method in PerlinNoise, plus it has less chance of
     * repetition in chunks because it uses a pseudo-random function (curiously,
     * {@link ThrustRNG#determine(long)}, which has rather good distribution and is
     * fast) instead of a number chosen by hash from a single 256-element array.
     *
     * @param xin X input; works well if between 0.0 and 1.0, but anything is
     *            accepted
     * @param yin Y input; works well if between 0.0 and 1.0, but anything is
     *            accepted
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static double noise(final double xin, final double yin, final int seed) {
	// xin *= epi;
	// yin *= epi;
	double noise0, noise1, noise2; // from the three corners
	// Skew the input space to figure out which simplex cell we're in
	final double skew = (xin + yin) * PerlinNoise.F2; // Hairy factor for 2D
	final int i = WhirlingNoise.fastFloor(xin + skew);
	final int j = WhirlingNoise.fastFloor(yin + skew);
	final double t = (i + j) * PerlinNoise.G2;
	final double X0 = i - t; // Unskew the cell origin back to (x,y) space
	final double Y0 = j - t;
	final double x0 = xin - X0; // The x,y distances from the cell origin
	final double y0 = yin - Y0;
	// For the 2D case, the simplex shape is an equilateral triangle.
	// determine which simplex we are in.
	int i1, j1; // Offsets for second (middle) corner of simplex in (i,j)
	// coords
	if (x0 > y0) {
	    i1 = 1;
	    j1 = 0;
	} // lower triangle, XY order: (0,0)->(1,0)->(1,1)
	else {
	    i1 = 0;
	    j1 = 1;
	} // upper triangle, YX order: (0,0)->(0,1)->(1,1)
	  // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
	  // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y),
	  // where
	  // c = (3-sqrt(3))/6
	final double x1 = x0 - i1 + PerlinNoise.G2; // Offsets for middle corner in (x,y)
	// unskewed coords
	final double y1 = y0 - j1 + PerlinNoise.G2;
	final double x2 = x0 - 1.0 + 2.0 * PerlinNoise.G2; // Offsets for last corner in (x,y)
	// unskewed coords
	final double y2 = y0 - 1.0 + 2.0 * PerlinNoise.G2;
	// Work out the hashed gradient indices of the three simplex corners
	/*
	 * int ii = i & 255; int jj = j & 255; int gi0 = perm[ii + perm[jj]] & 15; int
	 * gi1 = perm[ii + i1 + perm[jj + j1]] & 15; int gi2 = perm[ii + 1 + perm[jj +
	 * 1]] & 15;
	 */
	/*
	 * int hash = (int) rawNoise(i + (j * 0x9E3779B9), i + i1 + ((j + j1) *
	 * 0x9E3779B9), i + 1 + ((j + 1) * 0x9E3779B9), seed); int gi0 = hash & 15; int
	 * gi1 = (hash >>>= 4) & 15; int gi2 = (hash >>> 4) & 15;
	 */
	final int gi0 = (int) (ThrustRNG.determine(seed + i + ThrustRNG.determine(j)) & 15);
	final int gi1 = (int) (ThrustRNG.determine(seed + i + i1 + ThrustRNG.determine(j + j1)) & 15);
	final int gi2 = (int) (ThrustRNG.determine(seed + i + 1 + ThrustRNG.determine(j + 1)) & 15);
	// Calculate the contribution from the three corners
	double t0 = 0.5 - x0 * x0 - y0 * y0;
	if (t0 < 0) {
	    noise0 = 0.0;
	} else {
	    t0 *= t0;
	    noise0 = t0 * t0 * PerlinNoise.dot(PerlinNoise.phiGrad2[gi0], x0, y0);
	    // for 2D gradient
	}
	double t1 = 0.5 - x1 * x1 - y1 * y1;
	if (t1 < 0) {
	    noise1 = 0.0;
	} else {
	    t1 *= t1;
	    noise1 = t1 * t1 * PerlinNoise.dot(PerlinNoise.phiGrad2[gi1], x1, y1);
	}
	double t2 = 0.5 - x2 * x2 - y2 * y2;
	if (t2 < 0) {
	    noise2 = 0.0;
	} else {
	    t2 *= t2;
	    noise2 = t2 * t2 * PerlinNoise.dot(PerlinNoise.phiGrad2[gi2], x2, y2);
	}
	// Add contributions from each corner to get the final noise value.
	// The result is scaled to return values in the interval [-1,1].
	return 70.0 * (noise0 + noise1 + noise2);
    }

    /**
     * 2D simplex noise returning a float; extremely similar to
     * {@link #noise(double, double)}, but this may be slightly faster or slightly
     * slower. Unlike {@link PerlinNoise}, uses its parameters verbatim, so the
     * scale of the result will be different when passing the same arguments to
     * {@link PerlinNoise#noise(double, double)} and this method.
     *
     * @param x x input; works well if between 0.0 and 1.0, but anything is accepted
     * @param y y input; works well if between 0.0 and 1.0, but anything is accepted
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static float noiseAlt(final double x, final double y) {
	// xin *= epi;
	// yin *= epi;
	float noise0, noise1, noise2; // from the three corners
	final float xin = (float) x, yin = (float) y;
	// Skew the input space to figure out which simplex cell we're in
	final float skew = (xin + yin) * WhirlingNoise.F2f; // Hairy factor for 2D
	final int i = WhirlingNoise.fastFloor(xin + skew);
	final int j = WhirlingNoise.fastFloor(yin + skew);
	final float t = (i + j) * WhirlingNoise.G2f;
	final float X0 = i - t; // Unskew the cell origin back to (x,y) space
	final float Y0 = j - t;
	final float x0 = xin - X0; // The x,y distances from the cell origin
	final float y0 = yin - Y0;
	// For the 2D case, the simplex shape is an equilateral triangle.
	// determine which simplex we are in.
	int i1, j1; // Offsets for second (middle) corner of simplex in (i,j)
	// coords
	if (x0 > y0) {
	    i1 = 1;
	    j1 = 0;
	} // lower triangle, XY order: (0,0)->(1,0)->(1,1)
	else {
	    i1 = 0;
	    j1 = 1;
	} // upper triangle, YX order: (0,0)->(0,1)->(1,1)
	  // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
	  // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y),
	  // where
	  // c = (3-sqrt(3))/6
	final float x1 = x0 - i1 + WhirlingNoise.G2f; // Offsets for middle corner in (x,y)
	// unskewed coords
	final float y1 = y0 - j1 + WhirlingNoise.G2f;
	final float x2 = x0 - 1f + 2f * WhirlingNoise.G2f; // Offsets for last corner in (x,y)
	// unskewed coords
	final float y2 = y0 - 1f + 2f * WhirlingNoise.G2f;
	// Work out the hashed gradient indices of the three simplex corners
	/*
	 * int ii = i & 255; int jj = j & 255; int gi0 = perm[ii + perm[jj]] & 15; int
	 * gi1 = perm[ii + i1 + perm[jj + j1]] & 15; int gi2 = perm[ii + 1 + perm[jj +
	 * 1]] & 15;
	 */
	/*
	 * int hash = (int) rawNoise(i + (j * 0x9E3779B9), i + i1 + ((j + j1) *
	 * 0x9E3779B9), i + 1 + ((j + 1) * 0x9E3779B9), seed); int gi0 = hash & 15; int
	 * gi1 = (hash >>>= 4) & 15; int gi2 = (hash >>> 4) & 15;
	 */
	final int gi0 = (int) (ThrustRNG.determine(i + ThrustRNG.determine(j)) & 15);
	final int gi1 = (int) (ThrustRNG.determine(i + i1 + ThrustRNG.determine(j + j1)) & 15);
	final int gi2 = (int) (ThrustRNG.determine(i + 1 + ThrustRNG.determine(j + 1)) & 15);
	// Calculate the contribution from the three corners
	float t0 = 0.5f - x0 * x0 - y0 * y0;
	if (t0 < 0) {
	    noise0 = 0f;
	} else {
	    t0 *= t0;
	    // noise0 = t0 * t0 * dotterize(x0, y0, i, j);
	    noise0 = t0 * t0 * WhirlingNoise.dotf(WhirlingNoise.phiGrad2f[gi0], x0, y0);
	}
	float t1 = 0.5f - x1 * x1 - y1 * y1;
	if (t1 < 0) {
	    noise1 = 0f;
	} else {
	    t1 *= t1;
	    // noise1 = t1 * t1 * dotterize(x1, y1, i + i1, j + j1);
	    noise1 = t1 * t1 * WhirlingNoise.dotf(WhirlingNoise.phiGrad2f[gi1], x1, y1);
	}
	float t2 = 0.5f - x2 * x2 - y2 * y2;
	if (t2 < 0) {
	    noise2 = 0f;
	} else {
	    t2 *= t2;
	    // noise2 = t2 * t2 * dotterize(x2, y2, i+1, j+1);
	    noise2 = t2 * t2 * WhirlingNoise.dotf(WhirlingNoise.phiGrad2f[gi2], x2, y2);
	}
	// Add contributions from each corner to get the final noise value.
	// The result is scaled to return values in the interval [-1,1].
	return 70f * (noise0 + noise1 + noise2);
    }

    /**
     * 3D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double, double)} and this method. Roughly
     * 20-25% faster than the equivalent method in PerlinNoise, plus it has less
     * chance of repetition in chunks because it uses a pseudo-random function
     * (curiously, {@link ThrustRNG#determine(long)}, which has rather good
     * distribution and is fast) instead of a number chosen by hash from a single
     * 256-element array.
     *
     * @param xin X input
     * @param yin Y input
     * @param zin Z input
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static double noise(final double xin, final double yin, final double zin) {
	return WhirlingNoise.noise(xin, yin, zin, 123456789);
    }

    /**
     * 3D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double, double)} and this method. Roughly
     * 20-25% faster than the equivalent method in PerlinNoise, plus it has less
     * chance of repetition in chunks because it uses a pseudo-random function
     * (curiously, {@link ThrustRNG#determine(long)}, which has rather good
     * distribution and is fast) instead of a number chosen by hash from a single
     * 256-element array.
     *
     * @param xin X input
     * @param yin Y input
     * @param zin Z input
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static double noise(final double xin, final double yin, final double zin, final int seed) {
	// xin *= epi;
	// yin *= epi;
	// zin *= epi;
	double n0, n1, n2, n3; // Noise contributions from the four corners
	// Skew the input space to figure out which simplex cell we're in
	final double s = (xin + yin + zin) * PerlinNoise.F3; // Very nice and simple skew
	// factor for 3D
	final int i = WhirlingNoise.fastFloor(xin + s);
	final int j = WhirlingNoise.fastFloor(yin + s);
	final int k = WhirlingNoise.fastFloor(zin + s);
	final double t = (i + j + k) * PerlinNoise.G3;
	final double X0 = i - t; // Unskew the cell origin back to (x,y,z) space
	final double Y0 = j - t;
	final double Z0 = k - t;
	final double x0 = xin - X0; // The x,y,z distances from the cell origin
	final double y0 = yin - Y0;
	final double z0 = zin - Z0;
	// For the 3D case, the simplex shape is a slightly irregular
	// tetrahedron.
	// determine which simplex we are in.
	int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k)
	// coords
	int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k)
	// coords
	if (x0 >= y0) {
	    if (y0 >= z0) {
		i1 = 1;
		j1 = 0;
		k1 = 0;
		i2 = 1;
		j2 = 1;
		k2 = 0;
	    } // X Y Z order
	    else if (x0 >= z0) {
		i1 = 1;
		j1 = 0;
		k1 = 0;
		i2 = 1;
		j2 = 0;
		k2 = 1;
	    } // X Z Y order
	    else {
		i1 = 0;
		j1 = 0;
		k1 = 1;
		i2 = 1;
		j2 = 0;
		k2 = 1;
	    } // Z X Y order
	} else { // x0<y0
	    if (y0 < z0) {
		i1 = 0;
		j1 = 0;
		k1 = 1;
		i2 = 0;
		j2 = 1;
		k2 = 1;
	    } // Z Y X order
	    else if (x0 < z0) {
		i1 = 0;
		j1 = 1;
		k1 = 0;
		i2 = 0;
		j2 = 1;
		k2 = 1;
	    } // Y Z X order
	    else {
		i1 = 0;
		j1 = 1;
		k1 = 0;
		i2 = 1;
		j2 = 1;
		k2 = 0;
	    } // Y X Z order
	}
	// A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in
	// (x,y,z),
	// a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in
	// (x,y,z), and
	// a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in
	// (x,y,z), where
	// c = 1/6.
	final double x1 = x0 - i1 + PerlinNoise.G3; // Offsets for second corner in (x,y,z)
	// coords
	final double y1 = y0 - j1 + PerlinNoise.G3;
	final double z1 = z0 - k1 + PerlinNoise.G3;
	final double x2 = x0 - i2 + PerlinNoise.F3; // Offsets for third corner in
	// (x,y,z) coords
	final double y2 = y0 - j2 + PerlinNoise.F3;
	final double z2 = z0 - k2 + PerlinNoise.F3;
	final double x3 = x0 - 0.5; // Offsets for last corner in
	// (x,y,z) coords
	final double y3 = y0 - 0.5;
	final double z3 = z0 - 0.5;
	// Work out the hashed gradient indices of the four simplex corners
	/*
	 * int ii = i & 255; int jj = j & 255; int kk = k & 255;
	 *
	 * int gi0 = perm[ii + perm[jj + perm[kk]]] % 12; int gi1 = perm[ii + i1 +
	 * perm[jj + j1 + perm[kk + k1]]] % 12; int gi2 = perm[ii + i2 + perm[jj + j2 +
	 * perm[kk + k2]]] % 12; int gi3 = perm[ii + 1 + perm[jj + 1 + perm[kk + 1]]] %
	 * 12;
	 */
	final int gi0 = (int) (ThrustRNG.determine(seed + i + ThrustRNG.determine(j + ThrustRNG.determine(k))) & 31);
	final int gi1 = (int) (ThrustRNG
		.determine(seed + i + i1 + ThrustRNG.determine(j + j1 + ThrustRNG.determine(k + k1))) & 31);
	final int gi2 = (int) (ThrustRNG
		.determine(seed + i + i2 + ThrustRNG.determine(j + j2 + ThrustRNG.determine(k + k2))) & 31);
	final int gi3 = (int) (ThrustRNG
		.determine(seed + i + 1 + ThrustRNG.determine(j + 1 + ThrustRNG.determine(k + 1))) & 31);
	/*
	 * int hash = (int) rawNoise(i + ((j + k * 0x632BE5AB) * 0x9E3779B9), i + i1 +
	 * ((j + j1 + (k + k1) * 0x632BE5AB) * 0x9E3779B9), i + i2 + ((j + j2 + (k + k2)
	 * * 0x632BE5AB) * 0x9E3779B9), i + 1 + ((j + 1 + ((k + 1) * 0x632BE5AB)) *
	 * 0x9E3779B9), seed); int gi0 = (hash >>>= 4) % 12; int gi1 = (hash >>>= 4) %
	 * 12; int gi2 = (hash >>>= 4) % 12; int gi3 = (hash >>> 4) % 12;
	 */
	// int hash = (int) rawNoise(i, j, k, seed);
	// int gi0 = (hash >>>= 4) % 12, gi1 = (hash >>>= 4) % 12, gi2 = (hash >>>= 4) %
	// 12, gi3 = (hash >>>= 4) % 12;
	// Calculate the contribution from the four corners
	double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
	if (t0 < 0) {
	    n0 = 0.0;
	} else {
	    t0 *= t0;
	    n0 = t0 * t0 * WhirlingNoise.dot(WhirlingNoise.grad3f[gi0], x0, y0, z0);
	}
	double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
	if (t1 < 0) {
	    n1 = 0.0;
	} else {
	    t1 *= t1;
	    n1 = t1 * t1 * WhirlingNoise.dot(WhirlingNoise.grad3f[gi1], x1, y1, z1);
	}
	double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
	if (t2 < 0) {
	    n2 = 0.0;
	} else {
	    t2 *= t2;
	    n2 = t2 * t2 * WhirlingNoise.dot(WhirlingNoise.grad3f[gi2], x2, y2, z2);
	}
	double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
	if (t3 < 0) {
	    n3 = 0.0;
	} else {
	    t3 *= t3;
	    n3 = t3 * t3 * WhirlingNoise.dot(WhirlingNoise.grad3f[gi3], x3, y3, z3);
	}
	// Add contributions from each corner to get the final noise value.
	// The result is scaled to stay just inside [-1,1]
	return 32.0 * (n0 + n1 + n2 + n3);
    }

    /**
     * 3D simplex noise returning a float; extremely similar to
     * {@link #noise(double, double, double)}, but this may be slightly faster or
     * slightly slower. Unlike {@link PerlinNoise}, uses its parameters verbatim, so
     * the scale of the result will be different when passing the same arguments to
     * {@link PerlinNoise#noise(double, double, double)} and this method.
     *
     * @param x X input
     * @param y Y input
     * @param z Z input
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static float noiseAlt(final double x, final double y, final double z) {
	// xin *= epi;
	// yin *= epi;
	// zin *= epi;
	final float xin = (float) x, yin = (float) y, zin = (float) z;
	float n0, n1, n2, n3; // Noise contributions from the four corners
	// Skew the input space to figure out which simplex cell we're in
	final float s = (xin + yin + zin) * WhirlingNoise.F3f; // Very nice and simple skew
	// factor for 3D
	final int i = WhirlingNoise.fastFloor(xin + s);
	final int j = WhirlingNoise.fastFloor(yin + s);
	final int k = WhirlingNoise.fastFloor(zin + s);
	final float t = (i + j + k) * WhirlingNoise.G3f;
	final float X0 = i - t; // Unskew the cell origin back to (x,y,z) space
	final float Y0 = j - t;
	final float Z0 = k - t;
	final float x0 = xin - X0; // The x,y,z distances from the cell origin
	final float y0 = yin - Y0;
	final float z0 = zin - Z0;
	// For the 3D case, the simplex shape is a slightly irregular
	// tetrahedron.
	// determine which simplex we are in.
	int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k)
	// coords
	int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k)
	// coords
	if (x0 >= y0) {
	    if (y0 >= z0) {
		i1 = 1;
		j1 = 0;
		k1 = 0;
		i2 = 1;
		j2 = 1;
		k2 = 0;
	    } // X Y Z order
	    else if (x0 >= z0) {
		i1 = 1;
		j1 = 0;
		k1 = 0;
		i2 = 1;
		j2 = 0;
		k2 = 1;
	    } // X Z Y order
	    else {
		i1 = 0;
		j1 = 0;
		k1 = 1;
		i2 = 1;
		j2 = 0;
		k2 = 1;
	    } // Z X Y order
	} else { // x0<y0
	    if (y0 < z0) {
		i1 = 0;
		j1 = 0;
		k1 = 1;
		i2 = 0;
		j2 = 1;
		k2 = 1;
	    } // Z Y X order
	    else if (x0 < z0) {
		i1 = 0;
		j1 = 1;
		k1 = 0;
		i2 = 0;
		j2 = 1;
		k2 = 1;
	    } // Y Z X order
	    else {
		i1 = 0;
		j1 = 1;
		k1 = 0;
		i2 = 1;
		j2 = 1;
		k2 = 0;
	    } // Y X Z order
	}
	// A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in
	// (x,y,z),
	// a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in
	// (x,y,z), and
	// a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in
	// (x,y,z), where
	// c = 1/6.
	final float x1 = x0 - i1 + WhirlingNoise.G3f; // Offsets for second corner in (x,y,z)
	// coords
	final float y1 = y0 - j1 + WhirlingNoise.G3f;
	final float z1 = z0 - k1 + WhirlingNoise.G3f;
	final float x2 = x0 - i2 + WhirlingNoise.F3f; // Offsets for third corner in
	// (x,y,z) coords
	final float y2 = y0 - j2 + WhirlingNoise.F3f;
	final float z2 = z0 - k2 + WhirlingNoise.F3f;
	final float x3 = x0 - 0.5f; // Offsets for last corner in
	// (x,y,z) coords
	final float y3 = y0 - 0.5f;
	final float z3 = z0 - 0.5f;
	// Work out the hashed gradient indices of the four simplex corners
	/*
	 * int ii = i & 255; int jj = j & 255; int kk = k & 255;
	 *
	 * int gi0 = perm[ii + perm[jj + perm[kk]]] % 12; int gi1 = perm[ii + i1 +
	 * perm[jj + j1 + perm[kk + k1]]] % 12; int gi2 = perm[ii + i2 + perm[jj + j2 +
	 * perm[kk + k2]]] % 12; int gi3 = perm[ii + 1 + perm[jj + 1 + perm[kk + 1]]] %
	 * 12;
	 */
	final int gi0 = (int) (ThrustRNG.determine(i + ThrustRNG.determine(j + ThrustRNG.determine(k))) & 31);
	final int gi1 = (int) (ThrustRNG.determine(i + i1 + ThrustRNG.determine(j + j1 + ThrustRNG.determine(k + k1)))
		& 31);
	final int gi2 = (int) (ThrustRNG.determine(i + i2 + ThrustRNG.determine(j + j2 + ThrustRNG.determine(k + k2)))
		& 31);
	final int gi3 = (int) (ThrustRNG.determine(i + 1 + ThrustRNG.determine(j + 1 + ThrustRNG.determine(k + 1)))
		& 31);
//        int gi0 = determineBounded(i + determine(j + determine(k)), 92);
//        int gi1 = determineBounded(i + i1 + determine(j + j1 + determine(k + k1)), 92);
//        int gi2 = determineBounded(i + i2 + determine(j + j2 + determine(k + k2)), 92);
//        int gi3 = determineBounded(i + 1 + determine(j + 1 + determine(k + 1)), 92);
	/*
	 * int hash = (int) rawNoise(i + ((j + k * 0x632BE5AB) * 0x9E3779B9), i + i1 +
	 * ((j + j1 + (k + k1) * 0x632BE5AB) * 0x9E3779B9), i + i2 + ((j + j2 + (k + k2)
	 * * 0x632BE5AB) * 0x9E3779B9), i + 1 + ((j + 1 + ((k + 1) * 0x632BE5AB)) *
	 * 0x9E3779B9), seed); int gi0 = (hash >>>= 4) % 12; int gi1 = (hash >>>= 4) %
	 * 12; int gi2 = (hash >>>= 4) % 12; int gi3 = (hash >>> 4) % 12;
	 */
	// int hash = (int) rawNoise(i, j, k, seed);
	// int gi0 = (hash >>>= 4) % 12, gi1 = (hash >>>= 4) % 12, gi2 = (hash >>>= 4) %
	// 12, gi3 = (hash >>>= 4) % 12;
	// Calculate the contribution from the four corners
	float t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0;
	if (t0 < 0) {
	    n0 = 0f;
	} else {
	    t0 *= t0;
	    n0 = t0 * t0 * WhirlingNoise.dotf(WhirlingNoise.grad3f[gi0], x0, y0, z0);
	}
	float t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1;
	if (t1 < 0) {
	    n1 = 0f;
	} else {
	    t1 *= t1;
	    n1 = t1 * t1 * WhirlingNoise.dotf(WhirlingNoise.grad3f[gi1], x1, y1, z1);
	}
	float t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2;
	if (t2 < 0) {
	    n2 = 0f;
	} else {
	    t2 *= t2;
	    n2 = t2 * t2 * WhirlingNoise.dotf(WhirlingNoise.grad3f[gi2], x2, y2, z2);
	}
	float t3 = 0.6f - x3 * x3 - y3 * y3 - z3 * z3;
	if (t3 < 0) {
	    n3 = 0f;
	} else {
	    t3 *= t3;
	    n3 = t3 * t3 * WhirlingNoise.dotf(WhirlingNoise.grad3f[gi3], x3, y3, z3);
	}
	// Add contributions from each corner to get the final noise value.
	// The result is scaled to stay just inside [-1,1]
	return 32f * (n0 + n1 + n2 + n3);
    }

    /**
     * 4D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double, double, double)} and this method.
     * Roughly 20-25% faster than the equivalent method in PerlinNoise, plus it has
     * less chance of repetition in chunks because it uses a pseudo-random function
     * (curiously, {@link ThrustRNG#determine(long)}, which has rather good
     * distribution and is fast) instead of a number chosen by hash from a single
     * 256-element array.
     *
     * @param x X input
     * @param y Y input
     * @param z Z input
     * @param w W input (fourth-dimensional)
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static double noise(final double x, final double y, final double z, final double w) {
	return WhirlingNoise.noise(x, y, z, w, 123456789);
    }

    /**
     * 4D simplex noise. Unlike {@link PerlinNoise}, uses its parameters verbatim,
     * so the scale of the result will be different when passing the same arguments
     * to {@link PerlinNoise#noise(double, double, double, double)} and this method.
     * Roughly 20-25% faster than the equivalent method in PerlinNoise, plus it has
     * less chance of repetition in chunks because it uses a pseudo-random function
     * (curiously, {@link ThrustRNG#determine(long)}, which has rather good
     * distribution and is fast) instead of a number chosen by hash from a single
     * 256-element array.
     *
     * @param x    X input
     * @param y    Y input
     * @param z    Z input
     * @param w    W input (fourth-dimensional)
     * @param seed any int; will be used to completely alter the noise
     * @return noise from -1.0 to 1.0, inclusive
     */
    public static double noise(final double x, final double y, final double z, final double w, final int seed) {
	// The skewing and unskewing factors are hairy again for the 4D case
	// Skew the (x,y,z,w) space to figure out which cell of 24 simplices
	// we're in
	final double s = (x + y + z + w) * PerlinNoise.F4; // Factor for 4D skewing
	final int i = WhirlingNoise.fastFloor(x + s);
	final int j = WhirlingNoise.fastFloor(y + s);
	final int k = WhirlingNoise.fastFloor(z + s);
	final int l = WhirlingNoise.fastFloor(w + s);
	final double t = (i + j + k + l) * PerlinNoise.G4; // Factor for 4D unskewing
	final double X0 = i - t; // Unskew the cell origin back to (x,y,z,w) space
	final double Y0 = j - t;
	final double Z0 = k - t;
	final double W0 = l - t;
	final double x0 = x - X0; // The x,y,z,w distances from the cell origin
	final double y0 = y - Y0;
	final double z0 = z - Z0;
	final double w0 = w - W0;
	// For the 4D case, the simplex is a 4D shape I won't even try to
	// describe.
	// To find out which of the 24 possible simplices we're in, we need
	// to figure out the magnitude ordering of x0, y0, z0 and w0.
	// The method below is a good way of finding the ordering of x,y,z,w
	// and
	// then find the correct traversal order for the simplex we’re in.
	// First, six pair-wise comparisons are performed between each
	// possible pair
	// of the four coordinates, and the results are used to add up binary
	// bits
	// for an integer index.
	final int c = (x0 > y0 ? 32 : 0) | (x0 > z0 ? 16 : 0) | (y0 > z0 ? 8 : 0) | (x0 > w0 ? 4 : 0)
		| (y0 > w0 ? 2 : 0) | (z0 > w0 ? 1 : 0);
	// simplex[c] is a 4-vector with the numbers 0, 1, 2 and 3 in some
	// order.
	// Many values of c will never occur, since e.g. x>y>z>w makes x<z,
	// y<w and x<w
	// impossible. Only the 24 indices which have non-zero entries make
	// any sense.
	// We use a thresholding to set the coordinates in turn from the
	// largest magnitude.
	// The number 3 in the "simplex" array is at the position of the
	// largest coordinate.
	// The integer offsets for the second simplex corner
	final int i1 = PerlinNoise.simplex[c][0] >= 3 ? 1 : 0;
	final int j1 = PerlinNoise.simplex[c][1] >= 3 ? 1 : 0;
	final int k1 = PerlinNoise.simplex[c][2] >= 3 ? 1 : 0;
	final int l1 = PerlinNoise.simplex[c][3] >= 3 ? 1 : 0;
	// The number 2 in the "simplex" array is at the second largest
	// coordinate.
	// The integer offsets for the third simplex corner
	final int i2 = PerlinNoise.simplex[c][0] >= 2 ? 1 : 0;
	final int j2 = PerlinNoise.simplex[c][1] >= 2 ? 1 : 0;
	final int k2 = PerlinNoise.simplex[c][2] >= 2 ? 1 : 0;
	final int l2 = PerlinNoise.simplex[c][3] >= 2 ? 1 : 0;
	// The number 1 in the "simplex" array is at the second smallest
	// coordinate.
	// The integer offsets for the fourth simplex corner
	final int i3 = PerlinNoise.simplex[c][0] >= 1 ? 1 : 0;
	final int j3 = PerlinNoise.simplex[c][1] >= 1 ? 1 : 0;
	final int k3 = PerlinNoise.simplex[c][2] >= 1 ? 1 : 0;
	final int l3 = PerlinNoise.simplex[c][3] >= 1 ? 1 : 0;
	// The fifth corner has all coordinate offsets = 1, so no need to
	// look that up.
	final double x1 = x0 - i1 + PerlinNoise.G4; // Offsets for second corner in (x,y,z,w) coords
	final double y1 = y0 - j1 + PerlinNoise.G4;
	final double z1 = z0 - k1 + PerlinNoise.G4;
	final double w1 = w0 - l1 + PerlinNoise.G4;
	final double x2 = x0 - i2 + 2.0 * PerlinNoise.G4; // Offsets for third corner in (x,y,z,w) coords
	final double y2 = y0 - j2 + 2.0 * PerlinNoise.G4;
	final double z2 = z0 - k2 + 2.0 * PerlinNoise.G4;
	final double w2 = w0 - l2 + 2.0 * PerlinNoise.G4;
	final double x3 = x0 - i3 + 3.0 * PerlinNoise.G4; // Offsets for fourth corner in (x,y,z,w) coords
	final double y3 = y0 - j3 + 3.0 * PerlinNoise.G4;
	final double z3 = z0 - k3 + 3.0 * PerlinNoise.G4;
	final double w3 = w0 - l3 + 3.0 * PerlinNoise.G4;
	final double x4 = x0 - 1.0 + 4.0 * PerlinNoise.G4; // Offsets for last corner in (x,y,z,w) coords
	final double y4 = y0 - 1.0 + 4.0 * PerlinNoise.G4;
	final double z4 = z0 - 1.0 + 4.0 * PerlinNoise.G4;
	final double w4 = w0 - 1.0 + 4.0 * PerlinNoise.G4;
	final int gi0 = (int) (ThrustRNG
		.determine(seed + i + ThrustRNG.determine(j + ThrustRNG.determine(k + ThrustRNG.determine(l)))) & 255);
	final int gi1 = (int) (ThrustRNG.determine(
		seed + i + i1 + ThrustRNG.determine(j + j1 + ThrustRNG.determine(k + k1 + ThrustRNG.determine(l + l1))))
		& 255);
	final int gi2 = (int) (ThrustRNG.determine(
		seed + i + i2 + ThrustRNG.determine(j + j2 + ThrustRNG.determine(k + k2 + ThrustRNG.determine(l + l2))))
		& 255);
	final int gi3 = (int) (ThrustRNG.determine(
		seed + i + i3 + ThrustRNG.determine(j + j3 + ThrustRNG.determine(k + k3 + ThrustRNG.determine(l + l3))))
		& 255);
	final int gi4 = (int) (ThrustRNG.determine(
		seed + i + 1 + ThrustRNG.determine(j + 1 + ThrustRNG.determine(k + 1 + ThrustRNG.determine(l + 1))))
		& 255);
	// Noise contributions from the five corners are n0 to n4
	// Calculate the contribution from the five corners
	double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0, n0;
	if (t0 < 0) {
	    n0 = 0.0;
	} else {
	    t0 *= t0;
	    n0 = t0 * t0 * WhirlingNoise.dot(WhirlingNoise.grad4f[gi0], x0, y0, z0, w0);
	}
	double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1, n1;
	if (t1 < 0) {
	    n1 = 0.0;
	} else {
	    t1 *= t1;
	    n1 = t1 * t1 * WhirlingNoise.dot(WhirlingNoise.grad4f[gi1], x1, y1, z1, w1);
	}
	double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2, n2;
	if (t2 < 0) {
	    n2 = 0.0;
	} else {
	    t2 *= t2;
	    n2 = t2 * t2 * WhirlingNoise.dot(WhirlingNoise.grad4f[gi2], x2, y2, z2, w2);
	}
	double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3, n3;
	if (t3 < 0) {
	    n3 = 0.0;
	} else {
	    t3 *= t3;
	    n3 = t3 * t3 * WhirlingNoise.dot(WhirlingNoise.grad4f[gi3], x3, y3, z3, w3);
	}
	double t4 = 0.6 - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4, n4;
	if (t4 < 0) {
	    n4 = 0.0;
	} else {
	    t4 *= t4;
	    n4 = t4 * t4 * WhirlingNoise.dot(WhirlingNoise.grad4f[gi4], x4, y4, z4, w4);
	}
	// Sum up and scale the result to cover the range [-1,1]
	return 27.0 * (n0 + n1 + n2 + n3 + n4);
    }
    /*
     * public static void main(String[] args) { long hash; for (int x = -8; x < 8;
     * x++) { for (int y = -8; y < 8; y++) { hash = rawNoise(x, y, 1);
     * System.out.println("x=" + x + " y=" + y); System.out.println("normal=" +
     * (Float.intBitsToFloat(0x3F800000 | (int)(hash & 0x7FFFFF)) - 1.0));
     * System.out.println("tweaked=" + (Float.intBitsToFloat(0x40000000 | (int)(hash
     * & 0x7FFFFF)) - 3.0)); System.out.println("half=" +
     * (Float.intBitsToFloat(0x3F000000 | (int)(hash & 0x7FFFFF)) - 0.5)); } } }
     */
}
