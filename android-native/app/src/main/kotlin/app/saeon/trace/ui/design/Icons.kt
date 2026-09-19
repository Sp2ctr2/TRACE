package app.saeon.trace.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** One optical family of 24dp, 1.7-unit stroke vectors. No icon fonts or emoji. */
object BankIcons {
    private fun stroke(name: String, path: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = null, stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()
    val Back = stroke("뒤로", "M14 5L7 12L14 19")
    val Close = stroke("닫기", "M6 6L18 18M18 6L6 18")
    val Chevron = stroke("열기", "M9 5L16 12L9 19")
    val Home = stroke("홈", "M3 10L12 3L21 10M5 9V21H10V15H14V21H19V9")
    val Assets = stroke("자산", "M4 6H20V20H4ZM4 10H20M15 15H17M7 3H17")
    val Transfer = stroke("송금", "M4 8H20M15 3L20 8L15 13M20 17H4M9 12L4 17L9 22")
    val Shield = stroke("안전", "M12 3L20 6V12C20 17 16 20 12 22C8 20 4 17 4 12V6ZM8 12L11 15L16 9")
    val More = stroke("전체", "M4 5H20M4 12H20M4 19H20")
    val Bank = stroke("은행", "M3 8L12 3L21 8ZM3 21H21M5 10V18M10 10V18M14 10V18M19 10V18")
    val Bell = stroke("알림", "M5 17H19L17 14V9C17 6 15 4 12 4C9 4 7 6 7 9V14ZM10 21H14M12 2V4")
    val Profile = stroke("내 정보", "M16 7A4 4 0 1 1 8 7A4 4 0 1 1 16 7ZM4 21V19C4 15 8 13 12 13C16 13 20 15 20 19V21")
    val History = stroke("내역", "M4 3H20V21L16 19L12 21L8 19L4 21ZM8 8H16M8 12H16M8 16H12")
    val Search = stroke("검색", "M17 10A7 7 0 1 1 3 10A7 7 0 1 1 17 10ZM15 15L21 21")
    val Filter = stroke("필터", "M4 6H20M7 12H17M10 18H14")
    val Edit = stroke("수정", "M14 4L20 10M4 20L5 14L16 3L21 8L10 19ZM13 21H21")
    val Delete = stroke("지우기", "M8 5H21V19H8L2 12ZM12 9L18 15M18 9L12 15")
    val Check = stroke("완료", "M4 12L9 17L20 6")
    val Pause = stroke("보류", "M8 4V20M16 4V20")
    val Lock = stroke("잠금", "M5 10H19V21H5ZM8 10V6C8 1 16 1 16 6V10M12 14V17")
    val Link = stroke("연결", "M10 7L13 4C18 -1 25 6 20 11L17 14M14 17L11 20C6 25 -1 18 4 13L7 10M8 16L16 8")
    val Message = stroke("메시지", "M3 4H21V17H9L3 22ZM7 8H17M7 12H14")
    val Phone = stroke("연락", "M7 3L10 8L7 11C8 14 10 16 13 17L16 14L21 17L20 21C10 23 1 14 3 4Z")
    val Settings = stroke("설정", "M4 6H20M4 18H20M8 3V9M16 15V21M4 12H20")
    val Copy = stroke("복사", "M8 8H21V21H8ZM16 4V2H2V16H4")
    val Download = stroke("가져오기", "M12 3V15M7 10L12 15L17 10M4 17V21H20V17")
    val Share = stroke("공유", "M12 15V2M7 7L12 2L17 7M5 11H3V22H21V11H19")
    val Mic = stroke("음성 입력", "M9 5C9 1 15 1 15 5V12C15 16 9 16 9 12ZM5 10V12C5 21 19 21 19 12V10M12 19V23M8 23H16")
    val Eye = stroke("잔액 보기", "M2 12C7 3 17 3 22 12C17 21 7 21 2 12ZM15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12Z")
    val Calendar = stroke("예정", "M3 5H21V22H3ZM3 10H21M8 2V7M16 2V7M8 14H10M14 14H16M8 18H10")
    val Card = stroke("카드", "M3 5H21V20H3ZM3 10H21M7 15H11")
    val Arrow = stroke("다음", "M3 12H21M14 5L21 12L14 19")
    val Info = stroke("안내", "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12ZM12 11V17M12 7V7.1")
    val Trace: ImageVector = ImageVector.Builder("TRACE", 40.dp, 40.dp, 40f, 40f)
        .addPath(PathParser().parsePathString("M6 9h28v7H23.5v17h-7V16H6z M6 23h7v10H6z").toNodes(), fill = SolidColor(Color.Black)).build()
}
