package dev.eftrainer.data.content

data class IrregularVerb(
    val base: String,
    val pastSimple: List<String>,
    val pastParticiple: List<String>,
)

object IrregularVerbs {
    const val ID_PREFIX = "iv-"

    val list: List<IrregularVerb> =
        listOf(
            verb("be", listOf("was / were", "was/were", "was, were", "were / was", "was", "were"), listOf("been")),
            verb("become", "became", "become"),
            verb("begin", "began", "begun"),
            verb("break", "broke", "broken"),
            verb("bring", "brought"),
            verb("build", "built"),
            verb("buy", "bought"),
            verb("can", listOf("could"), emptyList()),
            verb("catch", "caught"),
            verb("choose", "chose", "chosen"),
            verb("come", "came", "come"),
            verb("cost", "cost"),
            verb("cut", "cut"),
            verb("do", "did", "done"),
            verb("dream", listOf("dreamt", "dreamed"), listOf("dreamt", "dreamed")),
            verb("drink", "drank", "drunk"),
            verb("drive", "drove", "driven"),
            verb("eat", "ate", "eaten"),
            verb("fall", "fell", "fallen"),
            verb("feel", "felt"),
            verb("find", "found"),
            verb("fly", "flew", "flown"),
            verb("forget", "forgot", "forgotten"),
            verb("get", "got"),
            verb("give", "gave", "given"),
            verb("go", "went", "gone"),
            verb("grow", "grew", "grown"),
            verb("have", "had"),
            verb("hear", "heard"),
            verb("hit", "hit"),
            verb("keep", "kept"),
            verb("know", "knew", "known"),
            verb("lay", "laid"),
            verb("learn", listOf("learnt", "learned"), listOf("learnt", "learned")),
            verb("leave", "left"),
            verb("lend", "lent"),
            verb("let", "let"),
            verb("lose", "lost"),
            verb("make", "made"),
            verb("meet", "met"),
            verb("pay", "paid"),
            verb("put", "put"),
            verb("read", "read"),
            verb("ring", "rang", "rung"),
            verb("run", "ran", "run"),
            verb("say", "said"),
            verb("see", "saw", "seen"),
            verb("sell", "sold"),
            verb("send", "sent"),
            verb("shut", "shut"),
            verb("sing", "sang", "sung"),
            verb("sit", "sat"),
            verb("sleep", "slept"),
            verb("speak", "spoke", "spoken"),
            verb("spend", "spent"),
            verb("stand", "stood"),
            verb("steal", "stole", "stolen"),
            verb("swim", "swam", "swum"),
            verb("take", "took", "taken"),
            verb("teach", "taught"),
            verb("tell", "told"),
            verb("think", "thought"),
            verb("throw", "threw", "thrown"),
            verb("understand", "understood"),
            verb("wake", "woke", "woken"),
            verb("wear", "wore", "worn"),
            verb("win", "won"),
            verb("write", "wrote", "written"),
        )

    val exercises: List<Exercise> = list.map { it.exercise() }

    val task: Task =
        Task(
            id = "iv",
            origin = Origin.SB,
            instruction = "Irregular verbs: write the past simple and past participle",
            exercises = exercises,
        )

    val section: Section =
        Section(
            id = "irregular-verbs",
            kind = SectionKind.GRAMMAR,
            title = "irregular verbs",
            reference = emptyList(),
            tasks = listOf(task),
        )

    val chapter: Chapter = Chapter(id = "Irregular verbs", title = "Student's Book p.164", sections = listOf(section))

    private val byId: Map<String, IrregularVerb> by lazy { list.associateBy { ID_PREFIX + it.base } }

    fun find(exerciseId: String): IrregularVerb? = byId[exerciseId]

    fun met(answered: Set<String>): List<IrregularVerb> = list.filter { ID_PREFIX + it.base in answered }

    private fun verb(
        base: String,
        pastSimple: String,
        pastParticiple: String = pastSimple,
    ) = IrregularVerb(base, listOf(pastSimple), listOf(pastParticiple))

    private fun verb(
        base: String,
        pastSimple: List<String>,
        pastParticiple: List<String>,
    ) = IrregularVerb(base, pastSimple, pastParticiple)

    private fun IrregularVerb.exercise(): Blank =
        Blank(
            id = ID_PREFIX + base,
            text = listOfNotNull(base, "{}", "{}".takeIf { pastParticiple.isNotEmpty() }).joinToString(" – "),
            gaps = listOfNotNull(pastSimple, pastParticiple.takeIf { it.isNotEmpty() }),
        )
}
