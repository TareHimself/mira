import org.koin.core.component.KoinComponent

interface FileRepository : KoinComponent {

    val getDataDir: String
}


