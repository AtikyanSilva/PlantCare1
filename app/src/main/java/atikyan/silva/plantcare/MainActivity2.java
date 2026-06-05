package atikyan.silva.plantcare;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity2 extends AppCompatActivity {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Bitmap lastSelectedBitmap;
    private FloatingActionButton fabCamera;
    private String currentMode = "detect";
    private String dailyAdviceFullText = "";
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final String[] KEYS_ADVICE   = { BuildConfig.GEMINI_KEY_ADVICE, BuildConfig.GEMINI_KEY_ADVICE2};
    private final String[] KEYS_DETECT   = { BuildConfig.GEMINI_KEY_DETECT,  BuildConfig.GEMINI_KEY_DETECT2};
    private final String[] KEYS_DIAGNOSE = { BuildConfig.GEMINI_KEY_DIAGNOSE, BuildConfig.GEMINI_KEY_DIAGNOSE2 };
    private final String[] KEYS_SEARCH   = { BuildConfig.GEMINI_KEY_SEARCH, BuildConfig.GEMINI_KEY_SEARCH2};

    private int idxAdvice   = 0;
    private int idxDetect   = 0;
    private int idxDiagnose = 0;
    private int idxSearch   = 0;

    private static final int CAMERA_PERMISSION_CODE = 101;
    private ProgressDialog progressDialog;

    private String nextKey(String[] keys, int[] idxHolder) {
        String key = keys[idxHolder[0] % keys.length];
        idxHolder[0] = (idxHolder[0] + 1) % keys.length;
        return key;
    }

    private String adviceKey()   { return KEYS_ADVICE  [idxAdvice   % KEYS_ADVICE.length];   }
    private String detectKey()   { return KEYS_DETECT  [idxDetect   % KEYS_DETECT.length];   }
    private String diagnoseKey() { return KEYS_DIAGNOSE[idxDiagnose % KEYS_DIAGNOSE.length]; }
    private String searchKey()   { return KEYS_SEARCH  [idxSearch   % KEYS_SEARCH.length];   }

    private void rotateAdvice()   { idxAdvice   = (idxAdvice   + 1) % KEYS_ADVICE.length;   }
    private void rotateDetect()   { idxDetect   = (idxDetect   + 1) % KEYS_DETECT.length;   }
    private void rotateDiagnose() { idxDiagnose = (idxDiagnose + 1) % KEYS_DIAGNOSE.length; }
    private void rotateSearch()   { idxSearch   = (idxSearch   + 1) % KEYS_SEARCH.length;   }

    private void showLoading(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void checkPermissionAndProceed() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showSourceSelectionDialog();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        RecyclerView rvCommonProblems = findViewById(R.id.rvCommonProblems);
        List<Problem> problemList = new ArrayList<>();

        problemList.add(new Problem("Желтеют листья",
                "Пожелтение листьев — один из самых частых сигналов тревоги у комнатных растений. Листья постепенно теряют насыщенный зелёный цвет, становятся бледно-жёлтыми или ярко-жёлтыми, иногда с зелёными прожилками.\n\nОсновные причины:\n• Перелив — корни не дышат в переувлажнённой почве и начинают гнить, из-за чего питательные вещества перестают поступать к листьям.\n• Недостаток азота — азот отвечает за хлорофилл, без него листья бледнеют снизу вверх.\n• Резкая смена условий — переезд, сквозняк, перепад температур.\n• Естественное старение — нижние листья желтеют и опадают — это норма.",
                "1. Потрогай почву: если мокрая — не поливай 7–10 дней, дай просохнуть.\n2. Проверь дренаж: в горшке должны быть отверстия, вода не должна стоять в поддоне.\n3. Если почва сухая и листья бледные — подкорми удобрением с азотом (мочевина, аммиачная селитра или комплексное удобрение).\n4. Убери растение от сквозняков и батарей.\n5. Пожелтевшие листья уже не восстановятся — аккуратно обрежь их у основания.", R.drawable.yellow_leaves));

        problemList.add(new Problem("Сухие кончики",
                "Кончики и края листьев буреют, засыхают и становятся хрупкими. Поражение начинается с кончика и постепенно ползёт вверх по листу. Сам лист при этом остаётся живым.\n\nОсновные причины:\n• Слишком сухой воздух — особенно зимой при работающем отоплении влажность падает до 20–30%, а большинству растений нужно 50–60%.\n• Жёсткая хлорированная вода — соли и хлор накапливаются в почве и обжигают корни и края листьев.\n• Избыток удобрений — солевой ожог корней даёт схожую картину.\n• Сквозняк — холодный воздух иссушает ткани листа.",
                "1. Поставь рядом с растением открытую ёмкость с водой или увлажнитель воздуха.\n2. Регулярно опрыскивай листья из пульверизатора — утром, чтобы вода успела высохнуть.\n3. Поливай только отстоянной или фильтрованной водой комнатной температуры — дай воде простоять минимум 8 часов.\n4. Раз в 2–3 месяца промывай почву большим количеством воды, чтобы вымыть соли.\n5. Обрежь засохшие кончики ножницами, повторяя форму листа — это не остановит причину, но улучшит внешний вид.", R.drawable.brown_tips));

        problemList.add(new Problem("Растение не растёт",
                "Растение стоит без изменений неделями и месяцами: нет новых листьев, побегов, точка роста не развивается. Иногда это норма в период покоя, но чаще — сигнал о проблеме.\n\nОсновные причины:\n• Недостаток света — без достаточного освещения фотосинтез замедляется, и у растения просто нет энергии для роста.\n• Тесный горшок — корни заняли весь объём, им некуда расти, питание не усваивается.\n• Нехватка питательных веществ — почва истощается через 1–2 года, удобрения необходимы.\n• Период покоя — зимой многие растения естественно замедляются.\n• Болезни корней — гниль или вредители в почве блокируют питание.",
                "1. Проверь освещение: большинству растений нужен яркий рассеянный свет 10–12 часов в сутки. Перемести ближе к окну или добавь фитолампу.\n2. Осмотри корни: если они вылезают из дренажных отверстий или закрутились по дну — пора пересаживать в горшок на 2–3 см больше.\n3. Начни регулярные подкормки: с весны по осень раз в 2 недели комплексным удобрением для комнатных растений.\n4. Зимой не стимулируй рост — это естественный отдых, просто обеспечь нормальное освещение и сократи полив.\n5. Вытащи растение из горшка и осмотри корни: здоровые — белые или светло-бежевые, больные — коричневые и мягкие.", R.drawable.no_growth));

        problemList.add(new Problem("Вялые листья",
                "Листья теряют упругость, становятся мягкими, поникают и выглядят \"уставшими\". Стебли тоже могут потерять тонус. Это один из самых тревожных признаков, требующих немедленного внимания.\n\nОсновные причины:\n• Пересохшая почва — клетки листьев теряют воду и сдуваются, как воздушный шарик.\n• Гниение корней от перелива — повреждённые корни не способны передавать воду, даже если почва мокрая.\n• Жара и прямое солнце — растение испаряет воду быстрее, чем корни успевают её поглощать.\n• Болезни сосудистой системы — грибковые инфекции типа фузариоза блокируют движение воды.",
                "1. Первым делом потрогай почву: сухая — немедленно полей, поставь горшок в таз с водой на 20–30 минут.\n2. Если почва мокрая, а листья вялые — проблема в корнях. Вытащи растение, осмотри корни: гнилые (тёмные, мягкие, с запахом) обрежь до здоровой ткани.\n3. После обрезки корней обработай срезы активированным углём или корицей, пересади в свежий субстрат.\n4. Убери растение от батарей и прямого солнца — обеспечь рассеянный свет и прохладу.\n5. После пересадки не поливай 3–4 дня, дай корням адаптироваться.", R.drawable.wilted_leaves));

        problemList.add(new Problem("Опадают листья",
                "Листья массово желтеют и опадают, либо опадают зелёными без видимых причин. Растение буквально лысеет на глазах. Это стрессовая реакция, и важно быстро найти причину.\n\nОсновные причины:\n• Резкая смена условий — переезд, перестановка, изменение освещения. Особенно чувствительны фикусы.\n• Сквозняки и холодный воздух — температура ниже 10–12°C вызывает шок у тропических растений.\n• Перепады температуры — близость к балконной двери или окну зимой.\n• Критический перелив или пересушка — экстремальный стресс.\n• Слишком тёмное место — растение сбрасывает листья, экономя ресурсы.",
                "1. Не перемещай растение — найди ему постоянное место и не трогай без крайней необходимости.\n2. Проверь температуру: минимум 16–18°C для большинства тропических видов, никаких сквозняков.\n3. Отодвинь от окна, если за окном мороз — стекло создаёт холодную зону.\n4. Нормализуй полив: дай почве просыхать между поливами на треть глубины.\n5. Не паникуй и не заливай — многие растения после стресса восстанавливаются при стабильных условиях. Дай 3–4 недели.", R.drawable.falling_leaves));

        problemList.add(new Problem("Пятна на листьях",
                "На листьях появляются пятна разного вида: коричневые, чёрные, жёлтые, белые или с тёмным ободком. По форме и цвету пятна можно определить причину.\n\nОсновные причины:\n• Грибковые болезни (альтернариоз, септориоз) — коричневые пятна с тёмным краем, часто с концентрическими кольцами. Развиваются при высокой влажности и плохой вентиляции.\n• Бактериальные инфекции — маслянистые, водянистые пятна, которые темнеют. Распространяются быстро.\n• Солнечные ожоги — белые или бежевые сухие пятна именно там, куда попадало прямое солнце.\n• Холодная вода при поливе — светлые пятна на листьях теплолюбивых растений.",
                "1. Определи тип пятна: сухое и светлое — ожог, мокрое и тёмное — бактерия или гриб.\n2. При грибке: удали все поражённые листья, обработай фунгицидом (Фитоспорин, Топаз), улучши вентиляцию.\n3. При бактериозе: срочно изолируй растение, удали поражённые части, обработай медьсодержащим препаратом.\n4. При ожогах: перемести от прямых лучей, поливай утром под корень, не на листья.\n5. Всегда поливай водой комнатной температуры — холодная вода оставляет пятна на листьях многих растений.", R.drawable.leaf_spots));

        problemList.add(new Problem("Плесень в горшке",
                "На поверхности почвы появляется белый, серый или зеленоватый налёт — пушистый или порошкообразный. Иногда сопровождается неприятным запахом сырости. Сама по себе плесень не убивает растение, но сигнализирует об опасных условиях.\n\nОсновные причины:\n• Хроническое переувлажнение — почва не успевает просыхать между поливами.\n• Плохой дренаж — вода застаивается в нижних слоях, создавая анаэробную среду.\n• Недостаток вентиляции — застойный воздух способствует размножению грибков.\n• Некачественный субстрат — торф без добавок быстро закисает.\n• Полив холодной водой в сочетании с тенью.",
                "1. Немедленно убери верхний слой почвы (2–3 см) с плесенью и замени свежим субстратом с добавлением перлита.\n2. Посыпь поверхность почвы толчёным активированным углём или корицей — природные антисептики.\n3. Сократи полив: поливай только когда верхние 3–4 см почвы полностью сухие.\n4. Переставь растение в более светлое и проветриваемое место.\n5. Проверь дренажные отверстия: они должны быть открыты, в поддоне не должна стоять вода.\n6. Раз в месяц поливай слабым раствором Фитоспорина — он подавляет грибки в почве.", R.drawable.mold_soil));

        problemList.add(new Problem("Вредители",
                "На растении появляются насекомые, липкий налёт (медвяная роса), паутина, белый пушок или деформированные листья. Вредители быстро размножаются и могут погубить растение за несколько недель.\n\nОсновные вредители:\n• Паутинный клещ — тонкая паутина под листьями, мелкие жёлтые точки на листьях. Любит сухой воздух.\n• Мучнистый червец — белые ватообразные комочки в пазухах листьев и на стеблях.\n• Щитовка — коричневые бляшки на стеблях, которые не смываются водой.\n• Тля — мелкие зелёные, чёрные или белые насекомые на молодых побегах.\n• Грибные комарики — мелкие мушки вокруг горшка, личинки в почве поедают корни.",
                "1. Немедленно изолируй поражённое растение от остальных.\n2. Смой вредителей под душем тёплой водой, уделив внимание нижней стороне листьев.\n3. Протри все листья мыльным раствором (1 ч.л. хозяйственного мыла на 1 л воды) или спиртом.\n4. Обработай системным инсектицидом: Актара, Фитоверм, Конфидор — по инструкции.\n5. Повторяй обработку каждые 5–7 дней минимум 3 раза — уничтожить нужно и взрослых особей, и яйца.\n6. Для профилактики раз в месяц протирай листья влажной тряпочкой и поддерживай высокую влажность воздуха.", R.drawable.pests));

        problemList.add(new Problem("Ожоги от солнца",
                "На листьях появляются белые, бежевые или светло-коричневые сухие пятна с чёткими границами. Они расположены именно там, куда попадали прямые солнечные лучи. Поражённая ткань отмирает и не восстанавливается.\n\nОсновные причины:\n• Прямые солнечные лучи в полуденные часы (с 11 до 16) — самые опасные для большинства комнатных растений.\n• Полив в солнечную погоду на листья — капли воды действуют как линзы и усиливают ожог.\n• Резкий перенос из тени на яркое солнце — растение не успевает адаптировать листья.\n• Отражение от светлых стен и зеркал — удваивает интенсивность света.",
                "1. Перемести растение от прямых лучей — большинству комнатных растений нужен яркий, но рассеянный свет.\n2. Притени окно: тюль, матовая плёнка или перемести горшок вглубь комнаты на 1–1,5 м.\n3. Никогда не поливай и не опрыскивай растения в солнечную погоду — только утром или вечером.\n4. Если хочешь приучить растение к большему свету — делай это постепенно, увеличивая освещение на 1 час в день.\n5. Обожжённые листья обрезать не обязательно — они продолжают работать. Удали только полностью сухие.", R.drawable.sunburn));

        problemList.add(new Problem("Недостаток света",
                "Растение вытягивается, стебли становятся тонкими и длинными, листья мельчают и бледнеют, расстояния между узлами увеличиваются. Пёстрые растения теряют рисунок, зелёные — становятся однотонно-бледными. Рост замедляется или прекращается.\n\nОсновные причины:\n• Тёмное расположение — слишком далеко от окна или окно выходит на север.\n• Короткий световой день зимой — менее 8 часов света недостаточно для активного роста.\n• Грязные окна — теряют до 30% светопропускания.\n• Загораживающие объекты — деревья, соседние здания, шторы.",
                "1. Перемести растение ближе к окну — оптимально на подоконник или в 0,5 м от него.\n2. Вымой окна — это быстро и бесплатно, а прирост света ощутимый.\n3. Используй фитолампу — полного спектра, на расстоянии 20–40 см, 12–14 часов в сутки. Таймер упростит жизнь.\n4. Поворачивай горшок на 90° каждые 1–2 недели, чтобы растение освещалось равномерно и не кренилось.\n5. Если растение сильно вытянулось — обрежь длинные побеги, это стимулирует боковой рост после нормализации освещения.", R.drawable.low_light));

        problemList.add(new Problem("Переизбыток воды",
                "Почва постоянно мокрая, листья желтеют (особенно нижние), стебли у основания размягчаются, появляется запах гнили. В запущенных случаях растение падает — корни сгнили и не держат его. Это одна из главных причин гибели комнатных растений.\n\nОсновные причины:\n• Слишком частый полив без учёта состояния почвы.\n• Отсутствие дренажных отверстий — вода не уходит.\n• Слишком большой горшок — почва долго не просыхает.\n• Тяжёлый субстрат без разрыхлителей — не пропускает воздух к корням.\n• Холодное время года — зимой потребность в воде снижается в 2–3 раза.",
                "1. Немедленно прекрати полив и дай почве максимально просохнуть.\n2. Вытащи растение из горшка и осмотри корни: гнилые (тёмные, мягкие, с неприятным запахом) обрежь стерильными ножницами до здоровой белой ткани.\n3. Обработай срезы порошком активированного угля или корицей.\n4. Пересади в свежий, хорошо дренированный субстрат (добавь перлит 1:3).\n5. В горшке обязательно должны быть отверстия, под горшок поставь дренажный слой из керамзита.\n6. Правило полива: вставь палец в почву на 3–4 см — если земля влажная, не поливай.", R.drawable.overwatering));

        problemList.add(new Problem("Недостаток воды",
                "Почва полностью сухая и отстаёт от стенок горшка, листья скручиваются трубочкой или складываются пополам, становятся морщинистыми и вялыми, кончики засыхают. При сильном обезвоживании листья опадают, стебли сморщиваются.\n\nОсновные причины:\n• Нерегулярный полив — про растение просто забывают.\n• Слишком маленький горшок — почва пересыхает за 1–2 дня.\n• Жара и прямое солнце — испарение воды резко возрастает.\n• Гидрофобная почва — старый торф отталкивает воду, она стекает по стенкам горшка, не увлажняя ком.\n• Корни полностью заняли горшок — воде некуда впитываться.",
                "1. Если почва очень сухая и вода скатывается — поставь горшок в таз с водой на 30–40 минут. Корни впитают столько, сколько нужно.\n2. После полного увлажнения дай воде стечь и вылей воду из поддона через 30 минут.\n3. Установи напоминание на телефоне или заведи привычку проверять растения раз в 2–3 дня.\n4. Если горшок стал лёгким — это верный сигнал пересохшей почвы.\n5. Мульчируй поверхность почвы — слой в 1–2 см из мха, гальки или декоративной щепы замедляет испарение в 2 раза.", R.drawable.underwatering));

        problemList.add(new Problem("Скручивание листьев",
                "Листья сворачиваются вдоль центральной жилки трубочкой, загибаются вниз или вверх, края подворачиваются. Скручивание — защитная реакция растения, направленная на уменьшение площади испарения.\n\nОсновные причины:\n• Недостаток влаги в почве — самая частая причина, растение защищается от обезвоживания.\n• Сухой воздух — особенно у тропических растений в отопительный сезон.\n• Вредители — паутинный клещ, тля и трипсы деформируют молодые листья при питании.\n• Вирусные болезни — листья скручиваются неравномерно, с мозаичным рисунком.\n• Избыток удобрений — солевой ожог корней нарушает водный баланс.",
                "1. Проверь почву: если сухая — полей, поставь в таз с водой на 20 минут для равномерного увлажнения.\n2. Осмотри нижнюю сторону листьев на вредителей: паутинный клещ оставляет тонкую паутину и мелкие точки, тля — мелкие насекомые на молодых побегах.\n3. Увеличь влажность воздуха: поставь рядом ёмкость с водой, используй увлажнитель или мокрый керамзит в поддоне.\n4. Если подозреваешь вирус — изолируй растение, поражённые листья удали, инструменты дезинфицируй.\n5. Проверь, не перекормлено ли растение: если давно вносил удобрения — промой почву чистой водой.", R.drawable.curling_leaves));

        rvCommonProblems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCommonProblems.setNestedScrollingEnabled(false);
        ProblemAdapter adapter = new ProblemAdapter(problemList);
        rvCommonProblems.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fabCamera = findViewById(R.id.fabCamera);
        fabCamera.setOnClickListener(v -> checkPermissionAndProceed());

        CardView cardDetect  = findViewById(R.id.cardDetect);
        CardView cardDiagnose = findViewById(R.id.cardDiagnose);
        CardView cardAdvice  = findViewById(R.id.cardAdvice);

        loadDailyAdvice(0);

        cardAdvice.setOnClickListener(v -> {
            if (!dailyAdviceFullText.isEmpty()) {
                showResultSheet(dailyAdviceFullText, false);
            } else {
                Toast.makeText(this, "Идея еще подготавливается...", Toast.LENGTH_SHORT).show();
            }
        });

        cardDetect.setOnClickListener(v -> {
            currentMode = "detect";
            checkPermissionAndProceed();
        });

        cardDiagnose.setOnClickListener(v -> {
            currentMode = "diagnose";
            checkPermissionAndProceed();
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                if (imageBitmap != null) {
                    lastSelectedBitmap = imageBitmap;
                    sendImageToAI(imageBitmap, 0);
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                executor.execute(() -> {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        InputStream probe = getContentResolver().openInputStream(uri);
                        BitmapFactory.decodeStream(probe, null, options);
                        if (probe != null) probe.close();

                        int maxDim = 1024;
                        int scale = 1;
                        while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) {
                            scale *= 2;
                        }
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = scale;

                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                        if (inputStream != null) inputStream.close();

                        if (bitmap != null) {
                            lastSelectedBitmap = bitmap;
                            sendImageToAI(bitmap, 0);
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });

        EditText searchBar = findViewById(R.id.searchBar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                String query = searchBar.getText().toString().trim();
                if (!query.isEmpty()) {
                    askAiQuestion(query, 0);
                    searchBar.setText("");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
        LinearLayout navHome        = findViewById(R.id.navHome);
        LinearLayout navInstruments = findViewById(R.id.navInstruments);
        LinearLayout navBotanist    = findViewById(R.id.navBotanist);
        LinearLayout navGarden      = findViewById(R.id.navGarden);

        navHome.setOnClickListener(v -> {});

        navInstruments.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity3.class));
            finish();
        });
        navBotanist.setOnClickListener(v -> {
            startActivity(new Intent(this, AiBotanistActivity.class));
            finish();
        });
        navGarden.setOnClickListener(v -> {
            startActivity(new Intent(this, MyGardenActivity.class));
            finish();
        });
    }

    private void loadDailyAdvice(int attempt) {

        SharedPreferences prefs = getSharedPreferences("plant_prefs", MODE_PRIVATE);
        String savedDate = prefs.getString("advice_date", "");
        String savedAdvice = prefs.getString("advice_text", "");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd",
                java.util.Locale.getDefault()).format(new java.util.Date());

        if (savedDate.equals(today) && !savedAdvice.isEmpty()) {

            dailyAdviceFullText = savedAdvice;
            String[] parts = savedAdvice.replace("*", "").split("\\|");
            TextView tvTitle = findViewById(R.id.tvAdviceTitle);
            if (tvTitle != null && parts.length >= 1) tvTitle.setText(parts[0].trim());
            return;
        }

        if (attempt >= KEYS_ADVICE.length) return;

        String key = adviceKey();
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", key);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        String prompt = "Предложи одну необычную идею для домашнего мини-огорода. Формат через '|': Название | Инструкция | Польза. Не используй markdown, звёздочки и заголовки.";
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String res = result.getText();
                runOnUiThread(() -> {
                    if (res != null && res.contains("|")) {
                        String clean = res.replace("*", "").replace("#", "").trim();
                        String[] parts = clean.split("\\|");
                        TextView tvTitle = findViewById(R.id.tvAdviceTitle);
                        if (tvTitle != null && parts.length >= 1)
                            tvTitle.setText(parts[0].trim());
                        dailyAdviceFullText = clean;

                        prefs.edit()
                                .putString("advice_date", today)
                                .putString("advice_text", clean)
                                .apply();
                    }
                });
            }
            @Override
            public void onFailure(Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "";
                if (msg.contains("429") || msg.contains("403")) {
                    rotateAdvice();
                    loadDailyAdvice(attempt + 1);
                }
            }
        }, executor);
    }

    private BottomSheetDialog activeTextSheet = null;
    private TextView activeTextSheetContent = null;

    private void askAiQuestion(String userText, int attempt) {
        if (attempt >= KEYS_SEARCH.length) {
            runOnUiThread(() -> Toast.makeText(this, "Все ключи поиска исчерпаны", Toast.LENGTH_SHORT).show());
            return;
        }

        if (attempt == 0) {
            runOnUiThread(this::showThinkingSheet);
        }

        String key = searchKey();
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", key);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText("Ты эксперт по растениям. Ответь: " + userText).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> updateThinkingSheet(result.getText()));
            }
            @Override
            public void onFailure(Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "";
                if (msg.contains("429") || msg.contains("403")) {
                    rotateSearch();
                    askAiQuestion(userText, attempt + 1);
                } else {
                    runOnUiThread(() -> updateThinkingSheet("Не удалось получить ответ. Попробуй ещё раз."));
                    t.printStackTrace();
                }
            }
        }, executor);
    }

    private void showThinkingSheet() {
        activeTextSheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_text, null);
        activeTextSheet.setContentView(sheetView);
        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        activeTextSheetContent = sheetView.findViewById(R.id.tvSheetContent);
        tvTitle.setText("Ответ ботаника");
        activeTextSheetContent.setText("🌿 Бот-ботаник думает...");
        activeTextSheet.show();
    }

    private void updateThinkingSheet(String text) {
        if (activeTextSheetContent != null) {
            activeTextSheetContent.setText(text != null ? text : "");
        }
    }

    private void sendImageToAI(Bitmap bitmap, int attempt) {
        boolean isDetect = currentMode.equals("detect");
        int maxAttempts = isDetect ? KEYS_DETECT.length : KEYS_DIAGNOSE.length;
        if (attempt >= maxAttempts) {
            runOnUiThread(() -> {
                hideLoading();
                Toast.makeText(this, "Все ключи исчерпаны", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        runOnUiThread(() -> showLoading("Анализирую фото... 🌱"));

        executor.execute(() -> {
            try {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                String prompt = isDetect
                        ? "Посмотри на изображение. Если это НЕ растение — ответь ровно одним словом: НЕ_РАСТЕНИЕ. Если это растение — напиши его название и подробное описание."
                        : "Посмотри на изображение. Если это НЕ растение — ответь ровно одним словом: НЕ_РАСТЕНИЕ. Если это растение — диагностируй проблемы и напиши лечение.";
                String key = isDetect ? detectKey() : diagnoseKey();

                GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", key);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);
                Content content = new Content.Builder().addImage(scaled).addText(prompt).build();
                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        runOnUiThread(() -> {
                            hideLoading();
                            showResultSheet(result.getText(), true);
                        });
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        String msg = t.getMessage() != null ? t.getMessage() : "";
                        runOnUiThread(() -> hideLoading());
                        if (msg.contains("429") || msg.contains("403")) {
                            if (isDetect) rotateDetect(); else rotateDiagnose();
                            sendImageToAI(bitmap, attempt + 1);
                        } else if (msg.contains("503") && attempt < 2) {
                            sendImageToAI(bitmap, attempt + 1);
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Ошибка анализа фото", Toast.LENGTH_SHORT).show());
                            t.printStackTrace();
                        }
                    }
                }, executor);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        });
    }

    private void showSourceSelectionDialog() {
        BottomSheetDialog sourceDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_select_source, null);
        view.findViewById(R.id.btnSourceCamera).setOnClickListener(v -> { openCamera(); sourceDialog.dismiss(); });
        view.findViewById(R.id.btnSourceGallery).setOnClickListener(v -> { galleryLauncher.launch("image/*"); sourceDialog.dismiss(); });
        sourceDialog.setContentView(view);
        sourceDialog.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) cameraLauncher.launch(takePictureIntent);
    }

    @SuppressLint("MissingInflatedId")
    private void showResultSheet(String text, boolean isPhotoResult) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        int layoutId = isPhotoResult ? R.layout.layout_bottom_sheet : R.layout.layout_bottom_sheet_text;
        View sheetView = getLayoutInflater().inflate(layoutId, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle   = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvContent = sheetView.findViewById(R.id.tvSheetContent);

        boolean isNotPlant = text != null && text.trim().contains("НЕ_РАСТЕНИЕ");

        if (isNotPlant) {
            tvTitle.setText("Не распознано");
            tvContent.setText("На фото не обнаружено растение. Попробуйте сфотографировать растение крупнее.");
        } else {
            tvTitle.setText(isPhotoResult ? (currentMode.equals("detect") ? "Распознавание" : "Диагностика") : "Ответ ботаника");
            tvContent.setText(text != null ? text.trim() : "");
        }

        if (isPhotoResult) {
            ImageView ivResult = sheetView.findViewById(R.id.ivPlantResult);
            if (ivResult != null && lastSelectedBitmap != null) ivResult.setImageBitmap(lastSelectedBitmap);

            View btnAddToGarden = sheetView.findViewById(R.id.btnAddToGarden);
            if (btnAddToGarden != null) {
                if (!isNotPlant && currentMode.equals("detect")) {
                    btnAddToGarden.setVisibility(View.VISIBLE);
                    btnAddToGarden.setOnClickListener(v -> {
                        bottomSheetDialog.dismiss();
                        Intent intent = new Intent(MainActivity2.this, AddPlantActivity.class);
                        if (lastSelectedBitmap != null) {
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            Bitmap scaled = Bitmap.createScaledBitmap(lastSelectedBitmap, 400, 400, true);
                            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            String photoBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
                            intent.putExtra("photo_base64", photoBase64);
                        }
                        startActivity(intent);
                    });
                } else {
                    btnAddToGarden.setVisibility(View.GONE);
                }
            }
        }
        bottomSheetDialog.show();
    }
}