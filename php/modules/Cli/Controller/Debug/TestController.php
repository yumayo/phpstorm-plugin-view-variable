<?php

namespace App\modules\Cli\Controller\Debug;

use App\Modules\Cli\Foundation\Controller;

class TestController extends Controller
{
    public function indexAction(): void
    {
        // 基本的な変数
        $intVar = 42;
        $floatVar = 3.14;
        $this->setVar('sum', $intVar + $floatVar . 'aaa');
    }
}
