package com.example.data.seed

import com.example.data.model.QuizQuestion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object AiAcademyCurriculaFoundations {

  fun buildBeginnersCurriculum(
    academy: SeedAcademySpec,
    courseTitle: String,
    slug: String,
    isFree: Boolean,
    level: String,
    primaryCategory: String,
    categories: List<String>
  ): GeneratedCourseCurriculum {
    val caseStudy1 = """
      **Setting:** A 14-staff district health clinic preparing quick-reference immunization summaries for newly hired nurses.
      **Challenge:** An administrator pasted an informal question ("Summarize national childhood booster intervals") into a public AI chat without attaching the Ministry manual. The reply sounded authoritative yet recommended an outdated 18-month booster gap instead of the updated 15-month schedule.
      **Workflow Applied:** The clinic switched to source-grounded prompting: pasting the exact Ministry PDF paragraph inside `<guideline>` tags, instructing the model to cite verbatim sentences only, and banning external memory assumptions.
      **Measured Outcome:** Onboarding handout preparation stayed 70% faster than writing manually while a 3-point human check caught discrepancies before any clinical sheet was printed.
    """.trimIndent()

    val caseStudy2 = """
      **Setting:** A 4-person Kampala bookkeeping practice handling monthly overdue-invoice follow-ups for 60 SME clients.
      **Challenge:** Junior accountants wanted AI help drafting firm yet polite payment reminders, but pasting client names, Tax IDs (TIN), phone numbers, and exact bank balances directly into public chats risked violating client confidentiality.
      **Workflow Applied:** The firm introduced a mandatory Red-Amber-Green rulebook and `[PLACEHOLDER]` masking (`[CLIENT_NAME]`, `[INVOICE_REF]`, `[DAYS_OVERDUE]`), using AI solely to refine message clarity and merging real figures offline inside their accounting tool.
      **Measured Outcome:** Drafting time dropped by 65% across weekly billing runs with zero personally identifiable information (PII) leaving company systems.
    """.trimIndent()

    val caseStudy3 = """
      **Setting:** An engineering lecturer and final-year research team compiling literature reviews on rural solar microgrids.
      **Challenge:** When asked directly for "5 peer-reviewed papers on East African mini-grid battery degradation with DOIs," the AI produced 5 realistic-looking references complete with plausible author surnames and journal years—yet verification showed 2 papers did not exist at all.
      **Workflow Applied:** The team adopted the 90-Second Grounding Routine: retrieving genuine PDF abstracts first from academic databases, supplying them in `<abstracts>` tags, and requiring an explicit `"List every claim or number not explicitly found in <abstracts>"` audit step.
      **Measured Outcome:** Zero fabricated citations reached the submitted journal manuscript, and literature synthesis speed still improved by roughly 2x.
    """.trimIndent()

    val caseStudy4 = """
      **Setting:** A regional fleet logistics coordinator overwhelmed by shifting vendor delivery emails and complex customs circulars.
      **Challenge:** Casual evening experiments ("Write a poem," "What's the best business?") produced novelty without saving real work time—leading to skepticism about whether AI was genuinely useful.
      **Workflow Applied:** Following a focused 30-day rulebook, the coordinator selected only two high-frequency workflows—translating dense shipping regulations into plain checklists and summarizing driver shift logs—paired with a non-negotiable "Signature Verification Rule."
      **Measured Outcome:** Morning dispatch briefing prep dropped by 35 minutes daily while driver compliance misunderstandings fell noticeably within one month.
    """.trimIndent()

    val lessons = listOf(
      GeneratedLessonSpec(
        suffix = "l1",
        moduleTitle = "Module 1: How Modern AI Works",
        title = "1. Tokens, Context Windows & Next-Word Prediction Explained",
        summary = "Understand the mechanical reality of Large Language Models—how tokenization, finite context attention, and probabilistic generation shape every reply.",
        orderIndex = 1L,
        durationMinutes = 35L,
        format = "WRITTEN",
        freePreview = true,
        notesMarkdown = """
          # How Modern AI Models Actually Generate Answers
          **Stage 1: Foundations** • *${academy.name}*

          ## Pedagogical Explanation: Prediction Engine, Not Fact Database
          When people first open an AI assistant, they often imagine a helpful librarian searching an internal verified encyclopedia. That mental model leads directly to costly mistakes. A **Large Language Model (LLM)** is actually an advanced **statistical sequence predictor** operating across three clear stages:

          1. **Tokenization (Reading in Sub-Word Pieces):** Rather than reading whole words like a human, the model breaks text into numeric pieces called **tokens**. Common short words (*"book"*, *"data"*) stay single tokens, while rarer or longer words (*"implementation"*, `"unbelievable"`) break into fragments (`["un", "believ", "able"]`). As a practical rule of thumb, **1,000 English tokens equals roughly 750 words**.
          2. **The Context Window (Temporary Working Desk):** Think of the **context window** as the model's physical desk. Everything in the active chat—your system rules, pasted excerpts, uploaded tables, questions, and previous replies—takes up space on that desk. Once a marathon conversation overflows the desk, earliest instructions slide off the edge and the model quietly stops obeying them.
          3. **Next-Token Probability (Fluent Continuation):** At every step, the model predicts *"Given all tokens currently visible on the desk, which token statistically fits next?"* Because it trained on billions of well-written pages, its grammar sounds smooth and authoritative even when specific facts are absent from its desk.

          ## Real-World Examples
          ### Example 1: Municipal Business Compliance Query (Before vs After)
          - **Unanchored Prompt:** `"What is the exact late-filing penalty for monthly VAT in Uganda?"`
            *What happens:* Without official text on the desk, the model blends tax rules remembered from multiple countries or old years and states an outdated fine confidently.
          - **Grounded Prompt:** `"Read ONLY the Tax Authority Notice excerpt below. State the exact monthly VAT late-filing penalty in one sentence. If the rate is missing from the excerpt, reply: 'Not specified in supplied notice.'"`
            *Why it works:* Putting the verified excerpt inside the context window steers token probabilities straight toward your real source text.

          ### Example 2: Customer Complaint Triage Across Long Threads
          - Suppose an operations clerk keeps one chat tab open all week for 40 different customer emails. By email #35, formatting rules set at email #1 get pushed out or diluted by noisy earlier threads.
          - **Best Practice:** Open a clean chat per distinct workflow so 100% of the context desk stays dedicated to your current rules and current document.

          ## Core Concepts Comparison Table
          | Mechanism | Plain-English Analogy | Practical Rule for Learners |
          |---|---|---|
          | **Token** | Puzzle piece of text (~3/4 word) | Concise, direct inputs run faster and leave room for source text |
          | **Context Window** | Temporary physical desk | Start fresh threads when switching tasks so rules stay visible |
          | **Probabilistic Output** | Educated continuation, not lookup | Fluent phrasing never proves factual truth—verify key facts |
          | **Grounding** | Placing the textbook open on the desk | Paste verified excerpts whenever accuracy on dates/rates matters |

          ## Common Misconceptions & Fixes
          - **Misconception:** *"If an AI reply uses exact percentages and formal headings, it must have verified them online."* **Fix:** Treat ungrounded first drafts as structural outlines only; verify dates, legal numbers, and rates against authoritative sources.
          - **Misconception:** *"Adding a friendly greeting at the top makes a 3,000-line messy chat reliable."* **Fix:** Trim noise and place crucial constraints right near the end or inside clear section headers.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Core Mental Model: LLMs predict probable next tokens—they do not query a verified database.",
          "Slide 2 — Token Mechanics: Text breaks into sub-word tokens (~1,000 tokens ≈ 750 English words).",
          "Slide 3 — Context Window Desk: Everything visible in the chat shares finite attention space.",
          "Slide 4 — Why Fluency Fools Readers: Polished grammar comes from language statistics, not fact checking.",
          "Slide 5 — Grounding Discipline: Place your verified excerpt directly on the desk before asking questions."
        ).joinToString("\n"),
        downloadTitle = "${slug}_ai_glossary_and_basics.txt",
        downloadContent = """
          AI FOR BEGINNERS — FOUNDATIONS FIELD GUIDE
          1. Token: ~4 characters or 3/4 of a word used as the model's atomic unit.
          2. Context Window: Finite temporary desk holding instructions, pasted sources, and chat history.
          3. Probabilistic Output: Predicting plausible continuations rather than guaranteed factual lookup.
          4. Grounding: Supplying verified source excerpts directly inside your prompt.
          5. Hallucination: Fluent, grammatical claim fabricated to continue a text pattern smoothly.
        """.trimIndent(),
        exercisePrompt = "Explain tokens, context windows, and grounding in 3 plain sentences, then rewrite one vague question into a source-grounded prompt.",
        keyTakeaways = listOf(
          "LLMs predict likely next tokens rather than looking up verified records in a database.",
          "The context window acts like a finite working desk—fresh threads prevent constraint drift.",
          "Fluent phrasing never proves accuracy; grounding in supplied text dramatically cuts errors."
        ),
        caseStudyText = caseStudy1
      ),
      GeneratedLessonSpec(
        suffix = "l2",
        moduleTitle = "Module 2: Safety, Privacy & Tool Choice",
        title = "2. Choosing AI Tools Safely: Privacy Hygiene & Limits",
        summary = "Apply the Red-Amber-Green data classification protocol, anonymize confidential fields safely, and match tasks to AI strengths.",
        orderIndex = 2L,
        durationMinutes = 35L,
        format = "SLIDES",
        freePreview = isFree,
        notesMarkdown = """
          # Data Privacy Hygiene & Choosing the Right AI Task
          **Stage 1: Foundations** • *${academy.name}*

          ## Pedagogical Explanation: Separating Structure Help From Secret Values
          Whenever you submit text to an external consumer chatbot, that payload travels across networks to remote servers. Unless your organization runs an explicitly configured zero-data-retention enterprise agreement, submitted chats may be logged for abuse monitoring or model improvement.

          You do **not** need to expose confidential records just to benefit from AI writing or analysis speed. The golden rule of AI privacy hygiene is **structural decoupling**: let AI improve *phrasing, logic, and templates* using anonymous placeholders, then fill real names and sensitive amounts back in **locally on your own computer**.

          ## The Red-Amber-Green Data Classification Model
          | Security Tier | Real-World Examples | Non-Negotiable Action |
          |---|---|---|
          | **Red (Strictly Banned)** | Passwords, API keys, bank PINs, national IDs, patient medical diagnoses, payroll numbers | **Never paste** into any public AI prompt |
          | **Amber (Mask First)** | Client names, email addresses, internal budgets, draft HR reviews, supplier contracts | Replace real values with `[CLIENT_A]`, `[INVOICE_NO]`, `[AMOUNT_TIER]` first |
          | **Green (Safe Direct Use)** | Public brochures, Excel formulas with fake data, study concepts, non-confidential outlines | Safe to use directly—always audit final output |

          ## Real-World Examples
          ### Example 1: Accounts Receivable Reminder (Healthcare / B2B Services)
          - **Risky Red-Tier Prompt:** `"Write a payment reminder to Grace Achieng (+256 772 555 190, TIN 10049281) at Nile Diagnostics for Invoice #INV-902 of UGX 4,200,000 overdue by 21 days."`
          - **Clean Masked Prompt:** `"Draft a polite, professional 110-word reminder to [CLIENT_CONTACT] at [COMPANY] about [INVOICE_ID], now 21 days overdue, requesting settlement confirmation by [FRIDAY_DATE]. Keep tone supportive."`
            *Result:* Identical persuasive polish in 5 seconds—zero private customer identity ever left your workstation.

          ### Example 2: Choosing Where AI Helps vs Hurts
          - **High Fit (Structure & Clarity):** Turning 10 messy workshop bullet notes into a clear agenda; explaining what a confusing Excel `#INDEX/MATCH` formula means step by step.
          - **Poor Fit (Sole Decision Authority):** Approving employee termination language unchecked; computing exact tax filings or medical dosages without specialist verification.

          ## Common Misconceptions & Fixes
          - **Misconception:** *"Saying 'Please keep this confidential' in the prompt makes a public chatbot legally private."* **Fix:** Prompts are just user messages—they do not override server logging or terms of service. Redact sensitive identifiers before pressing Send.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Structural Decoupling: Let AI polish phrasing and structure while real PII stays offline.",
          "Slide 2 — Red Tier (Never Paste): Credentials, PINs, IDs, patient files, and private financial numbers.",
          "Slide 3 — Amber Tier (Mask First): Replace real client names and figures with [CLIENT_A] tags.",
          "Slide 4 — Local Merge Step: Swap real details back inside your own email or accounting tool.",
          "Slide 5 — Task Fit Filter: Use AI heavily for clarity & drafting; keep legal/medical/tax sign-off human."
        ).joinToString("\n"),
        downloadTitle = "${slug}_privacy_redaction_checklist.txt",
        downloadContent = """
          RED-AMBER-GREEN SANITIZATION CHECKLIST
          [ ] Removed personal names, phone numbers, email addresses, and national IDs
          [ ] Removed passwords, PINs, bank account details, and API keys
          [ ] Swapped private client/company facts for [CLIENT_A] / [AMOUNT] placeholders
          [ ] Verified compliance terms, dates, and final figures offline before sending
        """.trimIndent(),
        exercisePrompt = "Mask 3 private identifiers inside a realistic work message using [PLACEHOLDER] tags so zero confidential data leaves your device.",
        keyTakeaways = listOf(
          "Never paste PII, credentials, health records, or bank numbers into public AI prompts.",
          "Replacing sensitive details with [PLACEHOLDER] tags gives full drafting leverage with zero exposure.",
          "Treat prompts as structure helpers while keeping high-risk legal, HR, and financial decisions human-led."
        ),
        caseStudyText = caseStudy2
      ),
      GeneratedLessonSpec(
        suffix = "l3",
        moduleTitle = "Module 3: Auditing & Source Grounding",
        title = "3. Spotting Hallucinations & Grounding Responses in Real Facts",
        summary = "Learn the four root causes of fabricated claims and apply the 90-Second Quote-and-Assumption Audit reliably.",
        orderIndex = 3L,
        durationMinutes = 40L,
        format = "WRITTEN",
        freePreview = isFree,
        notesMarkdown = """
          # Auditing AI Answers & Preventing Inventions
          **Stage 1: Foundations** • *${academy.name}*

          ## Pedagogical Explanation: Why Models Hallucinate Convinces Us
          When an AI generates a false statistic, non-existent court case, or invented URL, it is not trying to deceive you—and it cannot "feel" uncertainty the way a cautious human colleague does. Because training rewards smooth, helpful completion, a model faced with missing data stitches together **high-probability patterns matching what real citations or percentages normally look like**.

          Spotting hallucinations requires shifting from *"Does this sound intelligent?"* to systematic verification across **four common warning signals**:
          1. **Orphan Figures:** Exact numbers (`"68.2% improvement"`) appearing inside summaries even though your supplied text contained zero statistics.
          2. **Synthetic Citations:** Properly formatted author names, DOIs, or clean-looking URLs that lead to 404 pages or non-existent papers.
          3. **Sycophantic Premise Acceptance:** If your question embeds a false assumption (*"Explain why Kenya banned solar imports in 2024"*), an assistant often invents plausible-sounding arguments validating your false premise rather than pushing back.
          4. **Silent Assumptions:** When crucial inputs are omitted (currency, country, fiscal year), the model picks defaults silently without flagging its guess.

          ## Real-World Examples
          ### Example 1: Auditing a Supplier Warranty Summary (90-Second Audit)
          Suppose you ask an assistant to summarize a vendor's warranty PDF and get: *"Parts are covered for 24 months with free 48-hour courier replacement."*
          - **Apply Two-Step Verification Prompt:**
            1. `"For every obligation above, quote the exact verbatim sentence from <warranty_text> that supports it."`
            2. `"List every claim, deadline, or fee in your draft that does NOT appear verbatim inside <warranty_text>."`
          - *Discovery:* Step 2 immediately exposes `"48-hour free courier"` wasn't in the contract—the model inferred generic e-commerce language!

          ### Example 2: Avoiding Sycophancy on Technical Research
          - **Biased Prompt:** `"Give 3 reasons why SQL JOINs always delete duplicate rows."` -> Model risks confusing learners to sound agreeable.
          - **Neutral Verification Prompt:** `"Check whether the statement 'SQL INNER JOIN removes duplicate rows' is true or false. Explain with a tiny 2-row example."` -> Immediately gets the honest factual answer (False: JOINs multiply matching keys unless aggregated/deduplicated).

          ## Verification Playbook Table
          | Warning Signal | Quick Verification Test | Corrective Action |
          |---|---|---|
          | **Unsupplied Percentage** | Search your source doc (`Ctrl+F`) for that number | Verify in original sheet or delete claim |
          | **Named Policy / Citation** | Open the actual official link or journal index directly | Keep only independently opened sources |
          | **Suspected Silent Guess** | Ask *"What did you assume that wasn't stated?"* | Supply exact missing constraints & rerun |

          ## Common Misconceptions & Fixes
          - **Misconception:** *"If I ask 'Are you 100% sure?' the AI will double-check its facts accurately."* **Fix:** Asking `"Are you sure?"` often causes the model either to apologize unnecessarily or double down; always demand **verbatim source quotes** or check primary files yourself.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Why Inventions Happen: Pattern prediction fills factual voids with plausible-looking text.",
          "Slide 2 — Four Red Flags: Orphan numbers, synthetic URLs/DOIs, sycophancy, and silent assumptions.",
          "Slide 3 — Neutral Framing: Ask 'Verify whether X is true' instead of presupposing false statements.",
          "Slide 4 — Quote-Back Rule: Force verbatim quotes alongside every policy or contract claim.",
          "Slide 5 — Assumption Extraction: Explicitly demand 'List everything assumed or missing from source'."
        ).joinToString("\n"),
        downloadTitle = "${slug}_verification_log.csv",
        downloadContent = "claim_id,ai_claim_excerpt,source_checked,status,correction_applied\nC1,\"Booster interval is 18 months\",Ministry Schedule PDF p.12,Incorrect,Corrected to 15 months per verified manual\nC2,\"Paste sanitized outlines only\",Internal Privacy SOP v2,Verified,Approved as written",
        exercisePrompt = "Apply the 2-step Quote-Back and Assumption Audit to a short sample summary and log two claims requiring primary source checks.",
        keyTakeaways = listOf(
          "Hallucinations stem from pattern completion over missing facts rather than intentional error.",
          "Neutral questions prevent sycophantic agreement with incorrect assumptions.",
          "Demanding verbatim quotes and listing assumptions exposes ungrounded inventions in seconds."
        ),
        caseStudyText = caseStudy3
      ),
      GeneratedLessonSpec(
        suffix = "l4",
        moduleTitle = "Module 4: Personal AI Starter System",
        title = "4. Building Your Responsible First-30-Days AI Rulebook",
        summary = "Turn foundational AI habits into consistent real-world capability without sacrificing independent thinking or integrity.",
        orderIndex = 4L,
        durationMinutes = 35L,
        format = "SLIDES",
        freePreview = isFree,
        notesMarkdown = """
          # Your Responsible First-30-Days AI Rulebook
          **Stage 1: Foundations** • *${academy.name}*

          ## Pedagogical Explanation: Augmentation Over Cognitive Outsourcing
          Long-term value comes from **human judgment enhanced by AI speed**, never passive dependency. Learners who copy-paste whole essays or analyses without reading them lose their ability to spot weak logic—and eventually lose trust from managers, clients, and instructors when they cannot explain their own work under questioning.

          A sustainable personal rulebook rests on three simple disciplines practiced over your first 30 days:
          1. **Depth on Two Recurring Workflows:** Instead of trying 30 random gimmicks, commit to practicing just **two real weekly tasks** (for example: study concept review + workplace status emails) until your quality is fast and consistent.
          2. **Reflexive Sanitization:** Strip private identities and credentials automatically before any prompt leaves your screen.
          3. **The Signature Ownership Rule:** Treat your name at the bottom of an email, report, or code commit as a personal guarantee: *never send output you have not read, verified, and understood end-to-end.*

          ## Real-World Examples
          ### Example 1: Using AI as a Sparring Critic Instead of a Ghostwriter (Study & Career)
          - **Dependency Habit:** `"Write a 600-word cover letter for a Junior Data Analyst job."` -> Produces generic clichés indistinguishable from 200 other applicants.
          - **Growth Habit (Critic Mode):** Write your own rough 6-sentence note explaining two concrete projects you actually built, then ask: `"Act as a hiring manager. Point out the 3 weakest vague phrases in my draft below and suggest sharper questions I should answer—do NOT rewrite the whole letter yet."`
            *Result:* You stay 100% authentic and sharpen your real communication skills every week.

          ### Example 2: Progressive 30-Day Learning Cadence
          | Timeline | Focus Habit | Human Skill Developed |
          |---|---|---|
          | **Week 1** | Source-grounded summarizing & jargon translation | Rapid domain comprehension & fact verification |
          | **Week 2** | Red-Amber-Green masking on routine messages | Confidential communication speed |
          | **Weeks 3–4** | Critic Mode review of *your* drafts + 2 repeatable templates | Critical judgment & consistent quality control |

          ## Common Misconceptions & Fixes
          - **Misconception:** *"I need to bookmark 25 new AI websites every Monday to stay ahead."* **Fix:** Tools change every month, while core skills—clear specs, source grounding, privacy masking, and critical auditing—remain valuable regardless of platform.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Two-Workflow Rule: Build real fluency on two recurring tasks rather than chasing 20 apps.",
          "Slide 2 — Reflex Privacy Hygiene: Mask sensitive identities automatically on every prompt.",
          "Slide 3 — Signature Ownership: Only attach your name to facts and logic you can defend personally.",
          "Slide 4 — Critic Mode Learning: Let AI audit YOUR draft first to grow sharper judgment.",
          "Slide 5 — Stage 1 Complete: Advance to Stage 2 (AI for Productivity) with safe foundations locked."
        ).joinToString("\n"),
        downloadTitle = "${slug}_personal_ai_rulebook.txt",
        downloadContent = """
          STAGE 1 RESPONSIBLE AI RULEBOOK
          1. Two Weekly Anchor Workflows:
             - Workflow A:
             - Workflow B:
          2. Non-Negotiable Privacy Rule:
             - Strip names, IDs, credentials, and raw finances before prompting.
          3. Ownership Check Before Sharing:
             - Verify dates, figures, and citations against primary records.
        """.trimIndent(),
        exercisePrompt = "Define your 2 recurring anchor workflows for the next 30 days and write down your personal Signature Verification rule.",
        keyTakeaways = listOf(
          "Mastering two real repeating workflows builds deeper intuition than scattered experiments.",
          "Asking AI to critique your own draft strengthens analytical skill instead of replacing it.",
          "Consistent privacy masking and output ownership build lasting credibility."
        ),
        caseStudyText = caseStudy4
      )
    )

    val moduleQuestions = listOf(
      QuizQuestion(
        id = "mq1",
        type = "MULTIPLE_CHOICE",
        prompt = "Why can a Large Language Model generate a smooth, confident paragraph containing an invented statistic or outdated date?",
        options = listOf(
          "It predicts statistically plausible next tokens rather than querying a verified fact database",
          "It intentionally edits company databases overnight",
          "Tokens permanently lock out numbers above 100",
          "Context windows automatically verify legal accuracy"
        ),
        correctAnswer = "It predicts statistically plausible next tokens rather than querying a verified fact database",
        explanation = "LLMs generate fluent language via token probability; unless grounded in verified text you supply or checked externally, plausible phrasing can still be factually wrong.",
        points = 34
      ),
      QuizQuestion(
        id = "mq2",
        type = "TRUE_FALSE",
        prompt = "True or False: Replacing client names, phone numbers, and invoice IDs with placeholders like [CLIENT_A] lets you draft messages safely without exposing sensitive PII.",
        options = listOf("True", "False"),
        correctAnswer = "True",
        explanation = "True. Anonymizing sensitive fields before prompting preserves privacy while keeping full help with structure and tone.",
        points = 33
      ),
      QuizQuestion(
        id = "mq3",
        type = "MULTIPLE_CHOICE",
        prompt = "In our Red-Amber-Green privacy rule, how should you treat customer national IDs, bank details, and API passwords?",
        options = listOf(
          "Red — never paste them into public AI tools under any circumstance",
          "Green — share freely because AI chats delete everything instantly",
          "Amber — translate them into uppercase text first",
          "Publish them in slides for team training"
        ),
        correctAnswer = "Red — never paste them into public AI tools under any circumstance",
        explanation = "Sensitive personal data and credentials belong strictly in the Red category and must never leave secure authorized systems.",
        points = 33
      )
    )

    val finalQuestions = listOf(
      QuizQuestion(
        id = "q1",
        type = "MULTIPLE_CHOICE",
        prompt = "What is the AI 'context window' in everyday practical use?",
        options = listOf(
          "The active working memory holding your instructions, pasted reference text, and conversation history",
          "A permanent hard drive storing every file on your phone",
          "A paid antivirus plugin inside web browsers",
          "The monitor brightness calibration setting"
        ),
        correctAnswer = "The active working memory holding your instructions, pasted reference text, and conversation history",
        explanation = "Everything the model can currently attend to—instructions, snippets, and replies—lives inside its active context window.",
        points = 25
      ),
      QuizQuestion(
        id = "q2",
        type = "MULTIPLE_CHOICE",
        prompt = "Scenario: You paste a 2-page policy document and ask an AI assistant for a summary, then notice one deadline wasn't mentioned in your source document. What is the safest action?",
        options = listOf(
          "Verify the missing deadline directly against the original official document and remove unverified claims",
          "Keep the AI's guessed deadline because it sounds formal",
          "Shorten the title without reading the body text",
          "Forward immediately to staff and fix errors next quarter"
        ),
        correctAnswer = "Verify the missing deadline directly against the original official document and remove unverified claims",
        explanation = "Never trust ungrounded claims—especially dates and policy rules. Always reconcile them against authoritative primary records.",
        points = 25
      ),
      QuizQuestion(
        id = "q3",
        type = "TRUE_FALSE",
        prompt = "True or False: Asking an AI tool to answer strictly from pasted source excerpts and list any uncertainties helps prevent silent hallucinations.",
        options = listOf("True", "False"),
        correctAnswer = "True",
        explanation = "True. Grounding responses in supplied text and requesting explicit gaps exposes missing facts rather than letting the model invent filler.",
        points = 25
      ),
      QuizQuestion(
        id = "q4",
        type = "MULTIPLE_CHOICE",
        prompt = "Which workflow represents responsible beginner AI adoption in a workplace or school setting?",
        options = listOf(
          "Start with clear safe tasks, sanitize sensitive fields, verify all numbers and claims, and take ownership of the final work",
          "Paste full confidential customer spreadsheets and send replies unreviewed",
          "Rely solely on memorized buzzwords without testing outputs",
          "Use AI blindly for medical or legal decisions without specialist review"
        ),
        correctAnswer = "Start with clear safe tasks, sanitize sensitive fields, verify all numbers and claims, and take ownership of the final work",
        explanation = "Responsible adoption pairs speed with privacy hygiene and human verification so quality and trust stay intact.",
        points = 25
      )
    )

    return GeneratedCourseCurriculum(
      primaryCategory = primaryCategory,
      categories = categories,
      subtitle = "Stage 1 • Understand tokens, hallucinations, privacy hygiene & safe everyday AI use",
      description = "Start your AI Academy journey with clear explanations and zero hype. Learn how modern AI models process language, why hallucinations occur, how to protect sensitive data with Red-Amber-Green hygiene, and how to audit outputs reliably for study and work.",
      durationMinutes = lessons.sumOf { it.durationMinutes },
      lessonCount = lessons.size.toLong(),
      whatYouWillLearn = listOf(
        "Explain tokens, context windows, and probabilistic text generation clearly",
        "Protect personal and client information using Red-Amber-Green sanitization",
        "Spot AI hallucinations and anchor answers to verified primary documents",
        "Build a practical, responsible personal AI rulebook ready for Stage 2"
      ),
      prerequisites = listOf("Stage 1 Foundation — no technical or coding background required"),
      formats = listOf("WRITTEN", "SLIDES", "TEMPLATE", "QUIZ", "ASSIGNMENT"),
      tags = (categories + listOf("Stage 1", "AI Academy", "Foundations", "Safety", if (isFree) "Free" else "Premium")).distinct(),
      lessons = lessons,
      moduleQuizTitle = "Stage 1 Checkpoint: AI Basics & Privacy Safety",
      moduleQuizDescription = "Test your understanding of tokens, context windows, hallucination risks, and Red-Amber-Green privacy rules.",
      moduleQuizQuestionsJson = Json.encodeToString(moduleQuestions),
      quizTitle = "$courseTitle — Final Certification Exam",
      quizDescription = "Pass all 4 scenario and concept checks (70%+) to complete Stage 1 of the AI Academy Roadmap.",
      quizQuestionsJson = Json.encodeToString(finalQuestions),
      assignmentTitle = "Safe AI Audit & Personal Rulebook Submission",
      assignmentInstructions = "Complete a 3-part safety audit: (1) Take a sample work or study message with 3 private details and show how you anonymized it with [PLACEHOLDERS]; (2) Identify 2 factual claims in an AI summary that require primary verification; (3) State the top 2 tasks you will practice safely this week.",
      assignmentDeliverableFormat = "Text Audit + Safe Rulebook (100 pts)"
    )
  }

  fun buildProductivityCurriculum(
    academy: SeedAcademySpec,
    courseTitle: String,
    slug: String,
    isFree: Boolean,
    level: String,
    primaryCategory: String,
    categories: List<String>
  ): GeneratedCourseCurriculum {
    val caseStudy1 = """
      **Setting:** An operations coordinator at a Nairobi distribution hub spending 6+ hours every Monday on repetitive communications and table cleanup.
      **Challenge:** Trying to automate complex quarterly budget narratives failed because reviewing and correcting nuanced financial drafts took almost as long as writing manually.
      **Workflow Applied:** Applying the Net-Time-Saved Equation (`Manual Time − [Prompting + Verification]`), she eliminated low-ROI narrative tasks and standardized two high-frequency workflows where drafting was slow yet checking took under 60 seconds: daily WhatsApp summary extraction and client email replies.
      **Measured Outcome:** Weekly admin dropped from 6 hours to 95 minutes (over 4.5 hours saved weekly) with higher stakeholder satisfaction.
    """.trimIndent()

    val caseStudy2 = """
      **Setting:** A commercial construction project manager running three 45-minute site coordination calls per week.
      **Challenge:** Traditional prose meeting minutes buried critical supply tasks inside long paragraphs; roughly 30% of verbal commitments slipped because nobody noticed when an owner or due date was forgotten during the call.
      **Workflow Applied:** Enforced the 4-Bucket Action Extraction format (`Decisions Agreed`, `Action Owners Table`, `Open Questions`, and mandatory `[Owner TBD]` flags whenever notes omitted a name).
      **Measured Outcome:** Follow-up misses fell by 80% within three weeks, and site leads reviewed minutes in under 45 seconds instead of skimming full transcripts.
    """.trimIndent()

    val caseStudy3 = """
      **Setting:** A regional wholesale distributor reconciling 600 dealer invoices every Friday afternoon in Excel.
      **Challenge:** Trailing spaces from bank CSV exports repeatedly triggered `#N/A` errors inside legacy `VLOOKUP` sheets, stalling dispatch approvals every week.
      **Workflow Applied:** Prompted AI using exact tab names and column letters to build a `TRIM()`-normalized `XLOOKUP`/`IFERROR` formula, read the plain-English breakdown of each function layer, and ran a mandatory 3-Row Canary Test (valid row, trailing-space row, missing ID row) before dragging down.
      **Measured Outcome:** Friday formula breakage dropped to zero, saving 2.5 hours of manual troubleshooting each week.
    """.trimIndent()

    val caseStudy4 = """
      **Setting:** An independent marketing consultant managing 7 monthly retainer accounts simultaneously.
      **Challenge:** Retyping formatting constraints, word caps, and verification rules from scratch every Thursday afternoon created mental fatigue and inconsistent client reporting styles.
      **Workflow Applied:** Consolidated her three highest-leverage workflows into a lightweight Personal Snippet Vault organized by Trigger Moment (`After Client Sync`, `Thursday KPI Recap`, `Sheet Debug`), each paired with `[BRACKET]` placeholders and a 20-second pre-send check.
      **Measured Outcome:** Client reporting time dropped from 4.5 hours to 50 minutes weekly with uniform executive polish across all 7 accounts.
    """.trimIndent()

    val lessons = listOf(
      GeneratedLessonSpec(
        suffix = "l1",
        moduleTitle = "Module 1: High-ROI Time Auditing",
        title = "1. Saving 5+ Hours a Week Using the AI Task Matrix",
        summary = "Measure net workplace time saved accurately using Frequency × Verification Ease so AI removes real busywork instead of adding review overhead.",
        orderIndex = 1L,
        durationMinutes = 35L,
        format = "WRITTEN",
        freePreview = true,
        notesMarkdown = """
          # Reclaiming Time Without Sacrificing Quality
          **Stage 2: Workplace Velocity** • *${academy.name}*

          ## Pedagogical Explanation: The Net-Time-Saved Equation
          Adopting AI blindly across every corner of your calendar often backfires—spending 12 minutes crafting a prompt plus 18 minutes fixing subtle errors in a 25-minute task actually yields **negative productivity**. True workplace efficiency obeys a strict accounting formula:

          `Net Minutes Saved = Manual Time − (Prompt Setup Time + Verification Time)`

          Notice that **Verification Time**—not how fast the AI generates text—governs your real savings! Tasks deliver **5x to 10x leverage** when they satisfy three traits simultaneously:
          1. **High Weekly Frequency:** You repeat the chore at least 2–10 times per week, so small wins compound quickly.
          2. **Text / Structure Heavy:** Turning messy notes, lists, or table formats into structured outputs where generation is slow for humans.
          3. **Fast Verification (< 90 seconds):** Checking accuracy takes seconds (e.g., checking 4 extracted deadlines or reading a 100-word reply), unlike verifying nuanced legal prose or multi-sheet accounting figures.

          ## Real-World Examples
          ### Example 1: Scoring Weekly Office Tasks (Account Operations)
          - **Meeting Action Extraction (3x/wk):** Manual cleanup = `40 min`. Prompt paste + 90s audit = `5 min`. **Net saved: +35 min/call (+105 min/wk) -> Automate First.**
          - **Sensitive Performance Warning Email (Rare):** Writing delicately = `25 min`. Prompting + heavy emotional editing = `23 min`. **Net saved: +2 min + relationship risk -> Keep Human-Led.**

          ### Example 2: Spreadsheet Debugging vs Strategic Pricing Decisions
          - **Debugging an `#N/A` Lookup Formula:** Reading raw Excel help docs = `35 min`. Supplying column schema + testing 3 rows = `6 min`. **Net saved: +29 min -> High-ROI Co-Pilot.**
          - **Setting Final Quarterly Credit Terms:** Involves customer trust, cash liquidity risk, and contract judgment -> **Keep Human-Led** (use AI only to check clarity of your written summary).

          ## Task Prioritization Matrix
          | Task Quadrant | Everyday Examples | Recommended Strategy |
          |---|---|---|
          | **Frequent + Fast Check** | Meeting action tables, bullet-to-email replies, formula debugging, SOP outlines | **Build Saved Templates** first |
          | **Rare + Low Stakes** | Workshop icebreakers, event naming ideas | Run quick ad-hoc prompts only when needed |
          | **High Relationship / Legal Risk** | Payroll approval, layoffs, binding contracts, clinical notes | **Keep Human-Owned** for final judgment |

          ## Common Misconceptions & Fixes
          - **Misconception:** *"More sophisticated tasks always save more time."* **Fix:** Simple repetitive formatting chores done 10 times a week usually deliver 3x more reclaimed hours over a year than complex one-off drafts.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Net Savings Equation: Manual Time minus (Prompt Setup + Human Verification).",
          "Slide 2 — Verification Speed Wins: Fast-to-verify tasks give 5x–10x reliable workplace leverage.",
          "Slide 3 — Spot Hidden Time Traps: Avoid AI where fixing nuance takes as long as doing it manually.",
          "Slide 4 — Weekly Time Audit: Log 5 repeating tasks and their manual minutes before building templates.",
          "Slide 5 — Compound Effect: Reclaiming 90 minutes/week frees over 75 focused hours per year."
        ).joinToString("\n"),
        downloadTitle = "${slug}_weekly_time_audit.csv",
        downloadContent = "task_name,frequency_per_week,manual_minutes,verification_ease,ai_priority\nSummarize team call notes,3,40,High,Priority 1\nDraft client follow-up emails,10,12,High,Priority 1\nDebug spreadsheet lookup errors,2,35,Medium,Priority 2\nApprove final payroll figures,1,45,Low (Human Sign-off),Keep Manual Audit",
        exercisePrompt = "Score 4 repeating tasks from your week using Manual Minutes vs Verification Time to pinpoint your #1 Priority-1 workflow.",
        keyTakeaways = listOf(
          "True time savings equals manual duration minus prompting setup and verification time.",
          "Target frequent, structure-heavy chores where human checking takes under 90 seconds.",
          "Keep high-relationship, payroll, and compliance decisions firmly human-led."
        ),
        caseStudyText = caseStudy1
      ),
      GeneratedLessonSpec(
        suffix = "l2",
        moduleTitle = "Module 2: Meetings, Inbox & Briefs",
        title = "2. Turning Messy Notes Into Executive Briefs & Action Plans",
        summary = "Convert scattered voice dumps and long email threads into accountable Decisions + Owner/Deadline tables using the [TBD] guardrail.",
        orderIndex = 2L,
        durationMinutes = 40L,
        format = "SLIDES",
        freePreview = isFree,
        notesMarkdown = """
          # Eliminating Meeting & Inbox Overload
          **Stage 2: Workplace Velocity** • *${academy.name}*

          ## Pedagogical Explanation: Why Standard Summaries Hide Accountability
          When you ask an assistant to *"Summarize these meeting notes,"* it defaults to **narrative summary bias**—writing smooth prose like *"The team discussed warehouse stock levels and shared updates regarding Wednesday dispatch."* Narrative paragraphs obscure the only three questions busy colleagues care about after a call: **What did we agree? Who owns each next step? When is it due?**

          Worse, if notes are sloppy (`"Maybe Wednesday? Somebody call courier"`), models try to be "helpful" by guessing a team member's name or finalizing a tentative date. You prevent both failures cleanly using the **4-Bucket Extraction + `[TBD]` Guardrail**:
          1. **Decisions Agreed:** Only verified approvals—separate opinions from commitments.
          2. **Action Owners Table:** Columns `| Action Item | Owner | Due Date | Status / Risk |`.
          3. **Open Questions:** Unresolved items needing follow-up next session.
          4. **Strict Anti-Guessing Rule:** *"If an owner or deadline is ambiguous or missing in the notes, write `[Owner TBD]` or `[Date TBD]`—never guess."*

          ## Real-World Examples
          ### Example 1: Messy Logistics Call Dump -> Accountable Action Table
          - **Raw Input:** `"Sam checked depot stock, thinks Friday runs short. Grace noted supplier B wants deposit before dispatch, maybe Wednesday? Nobody confirmed who calls courier for Entebbe."`
          - **Structured Output:**
            - **Risk Flag:** Depot stock projected short by Friday.
            - `Call Entebbe Courier | [Owner TBD] | Urgent ([Date TBD]) | Unassigned`
            - `Confirm Supplier B Deposit Timing | Grace | Proposed Wed ([Confirm TBD]) | Awaiting confirmation`
            *Result:* Missing accountability jumps right off the screen in bold `[TBD]` tags during the final 30 seconds of your meeting!

          ### Example 2: 3-Bullet Intent -> Executive Inbox Reply
          Never write `"Reply politely to this client email"` (which yields bloated generic fluff and risks unwanted promises). Instead supply **your exact 3 commitments**:
          `"Target: Client Director | Cap: 110 words | Tone: Warm & direct | Decisions: (1) Approved Thu 10:00 EAT kickoff; (2) Need signed SOW first; (3) Includes max 2 revision rounds."`
          You keep 100% authority over business terms; AI handles crisp phrasing in seconds.

          ## Common Misconceptions & Fixes
          - **Misconception:** *"Longer meeting summaries look more thorough."* **Fix:** Executives and team leads act faster on a half-page table than a two-page prose essay.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Narrative Bias Problem: Prose paragraphs bury concrete owners, deadlines, and open risks.",
          "Slide 2 — Four-Bucket Output: Decisions Agreed, Owner/Deadline Table, Open Questions, and [TBD] flags.",
          "Slide 3 — Anti-Guessing Guardrail: Require [Owner TBD] whenever a field is vague or unstated.",
          "Slide 4 — Bullet-to-Email Pattern: Supply your exact terms in 3 bullets; let AI craft concise wording.",
          "Slide 5 — Thread Triage Habit: Condense 20-reply email chains into 'Action Needed Today' vs 'FYI Only'."
        ).joinToString("\n"),
        downloadTitle = "${slug}_meeting_and_inbox_snippets.txt",
        downloadContent = """
          SNIPPET 1 — ACTION-FIRST MEETING EXTRACTOR
          Convert sanitized notes below into:
          (A) Decisions Agreed (approvals only)
          (B) Markdown Table: | Action Item | Owner | Due Date | Blocker |
          (C) Open Unresolved Questions
          Rule: Never invent owners or dates. Print [Owner TBD] / [Date TBD] if missing.

          SNIPPET 2 — BULLET-TO-EMAIL DRAFTER
          Recipient: [Role] | Tone: Warm & executive | Cap: 120 words
          My Decisions & Facts:
          - [Bullet 1]
          - [Bullet 2]
          Exact Next Step: [Confirm by Day/Time]
        """.trimIndent(),
        exercisePrompt = "Transform a sanitized messy call note into the Decisions + Owner/[TBD] Action Table format.",
        keyTakeaways = listOf(
          "Action-Owner tables surface missing accountability instantly compared to narrative prose.",
          "Mandating [TBD] tags stops silent guessing of owners and deadlines.",
          "Bullet-to-email prompts keep 100% of business commitments under your direct control."
        ),
        caseStudyText = caseStudy2
      ),
      GeneratedLessonSpec(
        suffix = "l3",
        moduleTitle = "Module 3: Spreadsheets & Docs Accelerator",
        title = "3. Auditing Formulas, Restructuring Docs & Data Cleanup",
        summary = "Debug lookup errors, demand plain-language formula explanations, and run the 3-Row Canary Test before dragging logic across full sheets.",
        orderIndex = 3L,
        durationMinutes = 40L,
        format = "WRITTEN",
        freePreview = isFree,
        notesMarkdown = """
          # Spreadsheet & Document Speed-Ups Without Silent Formula Bugs
          **Stage 2: Workplace Velocity** • *${academy.name}*

          ## Pedagogical Explanation: Why Spreadsheet Prompts Break Without Schema Context
          Spreadsheets punish invisible mismatches silently: a trailing space copied from a web portal (`"C104 "`), a number formatted as plain text, or an Excel-only function pasted into Google Sheets can leave `#N/A` errors or subtly zero out rows across 1,000 invoices.

          To use AI safely as a spreadsheet co-pilot, follow the **Schema -> Plain Explanation -> 3-Row Canary Loop**:
          1. **Declare Exact Software & Schema:** Specify `"Excel 365"` vs `"Google Sheets"`, tab names (`Sales`, `Clients`), exact column letters (`A=ClientID, D=Amount`), and decimal/comma locale.
          2. **Require Plain-English Argument Walkthrough:** Instruct AI to explain each nested piece (*"What does the inner `TRIM()` do? What triggers `IFERROR()`?"*) so you never paste black-box formulas blindly.
          3. **Execute the 3-Row Canary Test:** Before dragging across 500 rows, test manually on three hand-verified rows:
             - *Row 1 (Normal Match):* Standard clean ID—does math match hand calculation?
             - *Row 2 (Dirty Key):* Add a trailing space `"C102 "` or text number—does lookup still match?
             - *Row 3 (Unknown / Blank ID):* Confirm clean `"Unmatched"` or `0` instead of `#N/A` poisoning downstream `SUM()` totals!

          ## Real-World Examples
          ### Example 1: Hardening a Failing Dealer Invoice Lookup
          - **Fragile Formula:** `=VLOOKUP(A2, Clients!A:C, 3, FALSE)` -> fails on trailing spaces or column insertions, and breaks total `SUM` cells with `#N/A`.
          - **Hardened Formula + Breakdown:**
            ```excel
            =IFERROR(XLOOKUP(TRIM(A2), INDEX(TRIM(Clients!A:A)), Clients!C:C), "Check ID")
            ```
            `TRIM(A2)` strips invisible whitespace, `XLOOKUP` aligns keys safely even if columns shift later, and `IFERROR` prints `"Check ID"` instead of corrupting totals.

          ### Example 2: Compressing a Dense 4-Page Policy Doc Into a 1-Page Executive Brief
          - Supply the sanitized text and request: `"(1) 3-line Executive Verdict; (2) Comparison Table (Current Rule | Updated Rule | Team Action Required); (3) Top 3 edge-case exceptions."`
          - *Verification:* Cross-check every dollar figure, percentage, and effective date against the source PDF before distributing.

          ## Common Mistakes & Fixes
          - **Mistake:** Asking `"Give me a formula to match sheets"` with no column letters or sample rows. **Fix:** Share two header lines and two fake sample rows every time so syntax matches your exact layout immediately.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Exact Schema First: Name Excel vs Google Sheets, tab names, and exact column letters.",
          "Slide 2 — Zero Black-Box Formulas: Require a plain-English breakdown of every nested function.",
          "Slide 3 — Common Dirty Data Bugs: Trailing spaces (fix via TRIM) and text-vs-number types.",
          "Slide 4 — The 3-Row Canary Test: Verify normal, dirty-space, and blank cases before dragging down.",
          "Slide 5 — Document Compression: Reformat dense technical policies into Action Comparison Tables."
        ).joinToString("\n"),
        downloadTitle = "${slug}_spreadsheet_prompt_kit.txt",
        downloadContent = """
          SPREADSHEET FORMULA CO-PILOT TEMPLATE
          Software: [Excel 365 / Google Sheets]
          Columns:
          - Tab 'Sales' A: InvoiceID | B: ClientID | C: Region | D: Amount
          - Tab 'Clients' A: ClientID | B: Tier | C: AccountOwner
          Goal: [Describe calculation or lookup]
          Edge Cases: Handle trailing spaces and return 0 / "Unmatched" instead of #N/A.
          Required Output:
          1) Exact formula
          2) Plain-English step-by-step explanation
          3) 3 canary test rows (Normal, Dirty Space, Missing Key)
        """.trimIndent(),
        exercisePrompt = "Describe a real 3-column lookup or summary task with schema context and trace how 3 canary rows verify edge cases.",
        keyTakeaways = listOf(
          "Declaring software, sheet tabs, and column letters eliminates incompatible formula guesses.",
          "Trailing spaces and mixed text/number types cause most silent lookup failures.",
          "Testing 3 canary rows (normal, dirty, missing) safeguards large spreadsheets before wide rollout."
        ),
        caseStudyText = caseStudy3
      ),
      GeneratedLessonSpec(
        suffix = "l4",
        moduleTitle = "Module 4: Reusable Personal SOP Library",
        title = "4. Packaging Your Top Prompts Into a Weekly SOP Playbook",
        summary = "Organize your highest-ROI prompts into a lean 3-to-5 Template Snippet Vault with triggers, slots, and 20-second verification checks.",
        orderIndex = 4L,
        durationMinutes = 35L,
        format = "SLIDES",
        freePreview = isFree,
        notesMarkdown = """
          # From One-Off Prompts to Repeatable Weekly SOPs
          **Stage 2: Workplace Velocity** • *${academy.name}*

          ## Pedagogical Explanation: Eliminating Daily Prompt Fatigue
          Retyping formatting rules, word caps, and `[TBD]` safety constraints from memory every time you start work introduces friction—and when you are rushed on Friday afternoon, it's easy to forget safety guardrails completely. High-performing practitioners convert proven workflows into a compact **Personal Snippet Vault** containing only **3 to 5 core Standard Operating Procedure (SOP) cards**.

          Every dependable SOP card locks in four exact ingredients:
          1. **Trigger Moment:** Named by *when* you open it (`"Right after weekly client check-in"`), never vague labels like `"Prompt #4"`.
          2. **Tested Skeleton:** Proven Role + Structure + Negative Constraints + Fallback rule saved once.
          3. **`[BRACKET]` Input Slots:** Only 1–3 swappable slots taking 15 seconds to fill.
          4. **20-Second Pre-Send Audit:** Explicit high-risk check specific to that workflow (dates, client names, currency figures).

          ## Real-World Examples
          ### Example 1: Weekly Client Status Card (`SOP #1: Thursday Recap`)
          - **Ad-Hoc Prompting:** Wastes 4 minutes retyping preferences and frequently produces 400 words missing owner names.
          - **Saved SOP Card:**
            - *Trigger:* Every Thursday at 16:00 before emailing retainer clients.
            - *Skeleton:* `"Using ONLY <sanitized_notes>, generate: (1) 2-line Progress Headline; (2) Table: | Deliverable | Status | Next Action | Owner | (print [TBD] if missing); (3) 1-line Client Decision Needed. Under 180 words."`
            - *20-Second Audit:* Confirm milestone dates and client name match project tracker.

          ### Example 2: Lean 3-Card Starter Vault Table
          | Vault Entry | Trigger Moment | Variable Slots | Mandatory 20-Second Audit |
          |---|---|---|---|
          | **Meeting Extractor** | After any 30+ min call | `[ATTENDEES]`, `<notes>` | Check every owner & `[TBD]` flag |
          | **3-Bullet Reply** | Any non-trivial inbox response | `[RECIPIENT]`, `[3_BULLETS]` | Confirm dates, pricing, commitments |
          | **Formula Co-Pilot** | Any broken sheet lookup | `[SOFTWARE]`, `[COLUMNS]` | Canary-test 3 representative rows |

          ## Common Mistakes & Fixes
          - **Mistake:** Building a 50-template folder downloaded from social media that you never open. **Fix:** Maintain only 3–5 battle-tested cards tied to real weekly habits, and prune unused drafts every month.
        """.trimIndent(),
        slidesText = listOf(
          "Slide 1 — Stop Rebuilding Prompts: Lock tested constraints into a compact 3-to-5 Template Vault.",
          "Slide 2 — Four SOP Ingredients: Trigger Moment + Skeleton + [BRACKET] Slots + 20-Second Audit.",
          "Slide 3 — Trigger-Based Naming: File templates by workflow moment ('After Call', 'Sheet Debug').",
          "Slide 4 — Built-In Safety Check: Pair every SOP with a specific 20-second error check.",
          "Slide 5 — Ready for Stage 3: Advance into delimiters, schemas & prompt engineering rigor."
        ).joinToString("\n"),
        downloadTitle = "${slug}_sop_vault_template.txt",
        downloadContent = """
          PERSONAL AI SOP VAULT CARD
          SOP Title:
          Trigger Moment (When Used):
          Estimated Weekly Minutes Saved:
          Copy-Paste Prompt Skeleton (with [SLOTS] & fallback rules):
          Mandatory 20-Second Verification Check:
        """.trimIndent(),
        exercisePrompt = "Document two repeatable weekly SOP cards using Trigger + Prompt Skeleton + [SLOTS] + 20-Second Audit.",
        keyTakeaways = listOf(
          "Saved SOP cards shrink multi-minute prompt writing into 15-second consistent execution.",
          "Attaching a specific 20-second audit keeps quality high even on rushed deadlines.",
          "A lean 3-to-5 template vault drives genuine daily adoption."
        ),
        caseStudyText = caseStudy4
      )
    )

    val moduleQuestions = listOf(
      QuizQuestion(
        id = "mq1",
        type = "MULTIPLE_CHOICE",
        prompt = "Which type of recurring task yields the highest safe productivity gain when paired with an AI workflow?",
        options = listOf(
          "Frequent text or structure-heavy work where reviewing output takes under a minute",
          "High-risk legal contracts shipped with zero review",
          "Rare tasks done once every five years",
          "Final sensitive HR terminations without human judgment"
        ),
        correctAnswer = "Frequent text or structure-heavy work where reviewing output takes under a minute",
        explanation = "High frequency + easy human verification maximizes saved hours while keeping error risk low.",
        points = 34
      ),
      QuizQuestion(
        id = "mq2",
        type = "TRUE_FALSE",
        prompt = "True or False: Instructing AI to print '[TBD]' when a meeting transcript lacks an explicit owner or deadline prevents fabricated assignments.",
        options = listOf("True", "False"),
        correctAnswer = "True",
        explanation = "True. Giving the model a clean fallback marker stops it from inventing fake names or dates.",
        points = 33
      ),
      QuizQuestion(
        id = "mq3",
        type = "MULTIPLE_CHOICE",
        prompt = "Why do bullet-point intent drafts ('3 key points -> polite email') work better than asking AI to 'reply professionally' with zero guidance?",
        options = listOf(
          "You retain 100% control of decisions, dates, and commitments while skipping repetitive phrasing work",
          "Short prompts force AI to inspect private bank accounts",
          "Bullet points turn emails into spreadsheets automatically",
          "It removes the need to read your email before hitting Send"
        ),
        correctAnswer = "You retain 100% control of decisions, dates, and commitments while skipping repetitive phrasing work",
        explanation = "Your bullets anchor every factual commitment while AI handles tone and clean prose cleanly.",
        points = 33
      )
    )

    val finalQuestions = listOf(
      QuizQuestion(
        id = "q1",
        type = "MULTIPLE_CHOICE",
        prompt = "Before copying an AI-suggested Excel or Google Sheets formula across 500 rows, what verification step must come first?",
        options = listOf(
          "Test the formula on 3 known rows—including a blank or dirty-space edge case—and inspect the result",
          "Hide the formula bar so colleagues cannot check references",
          "Delete raw input columns immediately",
          "Guess totals visually without recalculating"
        ),
        correctAnswer = "Test the formula on 3 known rows—including a blank or dirty-space edge case—and inspect the result",
        explanation = "Canary testing 3 representative rows proves formula compatibility and edge-case safety before wide rollout.",
        points = 25
      ),
      QuizQuestion(
        id = "q2",
        type = "MULTIPLE_CHOICE",
        prompt = "Scenario: You feed raw notes from a 45-minute project sync into an assistant to create an Action Table. How do you ensure accountability stays accurate?",
        options = listOf(
          "Require Decisions + Owner/Due Date columns with a '[TBD]' rule and confirm ambiguous rows with the team before sharing",
          "Let the model assign random team members to empty rows",
          "Send a 6-paragraph transcript unedited as final minutes",
          "Omit due dates so nobody feels pressured"
        ),
        correctAnswer = "Require Decisions + Owner/Due Date columns with a '[TBD]' rule and confirm ambiguous rows with the team before sharing",
        explanation = "Structured extraction plus honest '[TBD]' markers highlight missing ownership instead of faking accountability.",
        points = 25
      ),
      QuizQuestion(
        id = "q3",
        type = "TRUE_FALSE",
        prompt = "True or False: Saving your top 3 repeatable prompts as named SOP templates with [PLACEHOLDER] slots eliminates retyping instructions every day.",
        options = listOf("True", "False"),
        correctAnswer = "True",
        explanation = "True. Standard SOP templates turn ad-hoc prompting into fast, consistent daily execution.",
        points = 25
      ),
      QuizQuestion(
        id = "q4",
        type = "MULTIPLE_CHOICE",
        prompt = "What separates genuine AI workplace productivity from busywork?",
        options = listOf(
          "Measurable net hours saved on real recurring tasks paired with consistent output verification",
          "Generating longer emails with unnecessary corporate jargon",
          "Installing dozens of unused browser extensions",
          "Forwarding first drafts without proofreading numbers or tone"
        ),
        correctAnswer = "Measurable net hours saved on real recurring tasks paired with consistent output verification",
        explanation = "Real productivity is measured by reclaimed net time and preserved—or improved—output accuracy.",
        points = 25
      )
    )

    return GeneratedCourseCurriculum(
      primaryCategory = primaryCategory,
      categories = categories,
      subtitle = "Stage 2 • Save 5+ hours weekly across notes, inbox workflows, documents & spreadsheets",
      description = "Turn AI from a novelty into a disciplined workplace multiplier. Audit where your hours go each week, convert messy notes into executive action tables, safely debug spreadsheet formulas, and organize reusable SOP prompt templates.",
      durationMinutes = lessons.sumOf { it.durationMinutes },
      lessonCount = lessons.size.toLong(),
      whatYouWillLearn = listOf(
        "Audit weekly tasks using the Net-Time-Saved and Verification Ease matrix",
        "Extract executive decisions and [TBD]-guarded action tables from messy notes",
        "Generate, explain, and canary-test Excel/Sheets formulas without silent bugs",
        "Build a lean 3-to-5 template Personal Productivity SOP Vault"
      ),
      prerequisites = listOf("Suggested prerequisite: Stage 1 (Artificial Intelligence for Beginners)"),
      formats = listOf("WRITTEN", "SLIDES", "CSV", "QUIZ", "ASSIGNMENT"),
      tags = (categories + listOf("Stage 2", "AI Academy", "Productivity", "Workflows", if (isFree) "Free" else "Premium")).distinct(),
      lessons = lessons,
      moduleQuizTitle = "Stage 2 Checkpoint: Time Matrix & Executive Briefs",
      moduleQuizDescription = "Verify your grasp of high-ROI task selection, '[TBD]' action extraction, and fast inbox workflows.",
      moduleQuizQuestionsJson = Json.encodeToString(moduleQuestions),
      quizTitle = "$courseTitle — Final Certification Exam",
      quizDescription = "Pass all 4 applied workplace scenarios (70%+) to complete Stage 2 of AI Academy.",
      quizQuestionsJson = Json.encodeToString(finalQuestions),
      assignmentTitle = "Weekly Time-Audit & 2-Template Productivity Pack",
      assignmentInstructions = "Submit your Productivity Audit covering: (1) 3 recurring weekly tasks and baseline minutes spent; (2) Two reusable SOP prompt templates with [PLACEHOLDERS] and explicit verification rules; (3) Estimated weekly net minutes saved.",
      assignmentDeliverableFormat = "Time Audit + 2 SOP Templates (100 pts)"
    )
  }
}
