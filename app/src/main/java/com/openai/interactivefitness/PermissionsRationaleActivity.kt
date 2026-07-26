package com.openai.interactivefitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openai.interactivefitness.ui.theme.FitnessTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Health Connect 데이터 사용 안내",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "운동 세션, 걸음 수, 거리, 심박수와 소비 열량을 읽어 " +
                            "운동 기록과 추천을 개선합니다. 완료한 운동 세션만 Health Connect에 " +
                            "저장하며, 권한을 거부해도 수동 기록 기능은 계속 사용할 수 있습니다.",
                    )
                    Text(
                        "건강 데이터는 사용자의 기기와 연결된 저장소에서 처리되며 " +
                            "광고 목적으로 사용하지 않습니다.",
                    )
                    Button(onClick = ::finish) {
                        Text("확인")
                    }
                }
            }
        }
    }
}
